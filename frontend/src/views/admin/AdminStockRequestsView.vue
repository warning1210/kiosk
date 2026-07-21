<template>
  <main class="page">
    <AdminSidebar active="stock-requests" />

    <section class="content">
      <AdminPageHeader title="재고 신청" subtitle="지점에서 요청한 재고를 확인하고 승인·반려·배송 처리를 합니다." />

      <div class="summary">
        <AdminStatCard icon="📋" label="전체 신청" :value="`${summary.totalCount}건`" />
        <AdminStatCard icon="⏱" label="승인 대기" :value="`${summary.pendingCount}건`" delta="즉시 확인 필요" tone="orange" />
        <AdminStatCard icon="🚚" label="승인 후 진행" :value="`${summary.approvedCount}건`" delta="출고 준비 · 배송" tone="blue" />
        <AdminStatCard icon="✕" label="반려" :value="`${summary.rejectedCount}건`" tone="green" />
      </div>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>신청 목록</h2>
            <span>승인하면 출고 준비 상태가 되고, 배송 등록을 하면 지점이 수령 확인을 할 수 있습니다.</span>
          </div>
          <div class="tools">
            <select v-model="statusFilter" @change="reload">
              <option value="">전체 상태</option>
              <option v-for="status in statusOptions" :key="status" :value="status">{{ statusLabel(status) }}</option>
            </select>
            <select v-model="branchFilter" @change="reload">
              <option value="">전체 지점</option>
              <option v-for="branch in branches" :key="branch.branchId" :value="branch.branchId">{{ branch.branchName }}</option>
            </select>
            <label class="search">
              <span>⌕</span>
              <input v-model="keyword" placeholder="신청번호, 지점명, 맛 검색" @keyup.enter="reload">
            </label>
            <button class="ghost" type="button" @click="reload">검색</button>
          </div>
        </div>

        <div v-if="loading" class="empty">신청 내역을 불러오는 중입니다.</div>
        <div v-else-if="error" class="empty error-text">{{ error }}</div>
        <div v-else-if="!requests.length" class="empty">조건에 맞는 신청 내역이 없습니다.</div>

        <table v-else>
          <thead>
            <tr><th>신청번호</th><th>지점</th><th>신청 품목</th><th>긴급도</th><th>신청일</th><th>상태</th><th>처리</th></tr>
          </thead>
          <tbody>
            <tr v-for="request in requests" :key="request.stockRequestId">
              <td>
                <strong>{{ request.requestNumber }}</strong>
                <small class="sub">{{ request.requesterAdminName }}</small>
              </td>
              <td>{{ request.branchName }}</td>
              <td>
                <div class="items">
                  <span v-for="item in request.items" :key="item.flavorId" class="item-chip">
                    {{ item.flavorName }} <b>{{ item.approvedQuantity ?? item.requestedQuantity }}통</b>
                  </span>
                </div>
                <small v-if="request.requestReason" class="reason">사유 · {{ request.requestReason }}</small>
                <small v-if="request.rejectionReason" class="reject">반려 사유 · {{ request.rejectionReason }}</small>
                <small v-if="request.trackingNumber" class="tracking">
                  운송장 {{ request.trackingNumber }}<template v-if="request.courierName"> · {{ request.courierName }}</template>
                </small>
              </td>
              <td><span class="urgency" :class="request.urgency">{{ urgencyLabel(request.urgency) }}</span></td>
              <td>
                {{ formatDate(request.requestedAt) }}
                <small v-if="request.estimatedArrivalAt" class="sub">도착예정 {{ formatDate(request.estimatedArrivalAt) }}</small>
              </td>
              <td><span class="status" :class="request.requestStatus">{{ statusLabel(request.requestStatus) }}</span></td>
              <td>
                <div v-if="request.requestStatus === 'PENDING'" class="row-actions">
                  <button class="approve" type="button" :disabled="busyId === request.stockRequestId" @click="approve(request)">승인</button>
                  <button class="reject-btn" type="button" :disabled="busyId === request.stockRequestId" @click="openReject(request)">반려</button>
                </div>
                <button v-else-if="request.requestStatus === 'PREPARING'" class="track" type="button"
                        :disabled="busyId === request.stockRequestId" @click="openShip(request)">
                  배송 등록
                </button>
                <span v-else-if="request.requestStatus === 'SHIPPING'" class="muted">지점 수령 확인 대기</span>
                <span v-else class="muted">-</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!loading && requests.length" class="pagination-foot">
          <span>전체 {{ totalElements }}건 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="totalElements" :page-size="pageSize" />
        </div>
      </section>
    </section>

    <!-- 반려 -->
    <div v-if="rejectTarget" class="modal-backdrop" @click.self="rejectTarget = null">
      <section class="modal">
        <h2>신청 반려</h2>
        <p class="current">{{ rejectTarget.branchName }} · {{ rejectTarget.requestNumber }}</p>
        <label>
          반려 사유 <em>필수</em>
          <textarea v-model="rejectReason" rows="3" placeholder="예: 본사 재고 부족으로 다음 주 재신청 바랍니다"></textarea>
        </label>
        <p v-if="modalError" class="form-error">{{ modalError }}</p>
        <div class="modal-actions">
          <button type="button" @click="rejectTarget = null">취소</button>
          <button class="danger" type="button" :disabled="submitting" @click="submitReject">
            {{ submitting ? '처리 중...' : '반려하기' }}
          </button>
        </div>
      </section>
    </div>

    <!-- 배송 등록 -->
    <div v-if="shipTarget" class="modal-backdrop" @click.self="shipTarget = null">
      <section class="modal">
        <h2>배송 등록</h2>
        <p class="current">{{ shipTarget.branchName }} · {{ shipTarget.requestNumber }}</p>
        <label>운송장번호 <em>필수</em><input v-model="shipForm.trackingNumber" type="text" placeholder="1234-5678-9012"></label>
        <div class="two">
          <label>택배사<input v-model="shipForm.courierName" type="text" placeholder="CJ대한통운"></label>
          <label>배송 기사<input v-model="shipForm.driverName" type="text" placeholder="김기사"></label>
        </div>
        <label>도착 예정일시<input v-model="shipForm.estimatedArrivalAt" type="datetime-local"></label>
        <p v-if="modalError" class="form-error">{{ modalError }}</p>
        <div class="modal-actions">
          <button type="button" @click="shipTarget = null">취소</button>
          <button class="primary" type="button" :disabled="submitting" @click="submitShip">
            {{ submitting ? '처리 중...' : '배송 등록' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

const statusOptions = ['PENDING', 'PREPARING', 'SHIPPING', 'DELIVERED', 'REJECTED', 'CLOSED']

const requests = ref([])
const branches = ref([])
const summary = ref({ totalCount: 0, pendingCount: 0, approvedCount: 0, rejectedCount: 0 })
const loading = ref(true)
const error = ref('')
const toast = ref('')
const busyId = ref(null)

const statusFilter = ref('')
const branchFilter = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const totalElements = ref(0)

const rejectTarget = ref(null)
const rejectReason = ref('')
const shipTarget = ref(null)
const shipForm = ref({ trackingNumber: '', courierName: '', driverName: '', estimatedArrivalAt: '' })
const submitting = ref(false)
const modalError = ref('')

const pageStart = computed(() => (totalElements.value ? (page.value - 1) * pageSize + 1 : 0))
const pageEnd = computed(() => Math.min(page.value * pageSize, totalElements.value))

// 페이지 번호가 바뀌면 서버에서 그 페이지를 다시 받아 온다 (목록 전체를 들고 있지 않는다).
watch(page, load)

function statusLabel(status) {
  return ({
    PENDING: '승인 대기', APPROVED: '승인됨', PREPARING: '출고 준비',
    SHIPPING: '배송 중', DELIVERED: '수령 완료', REJECTED: '반려', CLOSED: '종료'
  })[status] || status
}
function urgencyLabel(urgency) {
  return ({ LOW: '여유', NORMAL: '보통', HIGH: '긴급' })[urgency] || urgency
}
function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' })
}
function showToast(message) {
  toast.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => { toast.value = '' }, 2800)
}

/** 필터를 바꿨을 때는 1페이지부터 다시 본다. */
function reload() {
  if (page.value === 1) load()
  else page.value = 1
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value - 1, size: pageSize }
    if (statusFilter.value) params.status = statusFilter.value
    if (branchFilter.value) params.branchId = branchFilter.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const { data } = await http.get('/hq/stock-requests', { params })
    requests.value = data.content ?? []
    totalElements.value = data.totalElements ?? 0
  } catch (e) {
    error.value = e.response?.data?.message || '신청 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    summary.value = (await http.get('/hq/stock-requests/summary')).data
  } catch (e) {
    console.error(e)
  }
}

async function loadBranches() {
  try {
    branches.value = (await http.get('/hq/branches')).data
  } catch (e) {
    console.error(e)
  }
}

async function refreshAll() {
  await Promise.all([load(), loadSummary()])
}

async function approve(request) {
  if (!window.confirm(`${request.branchName}의 ${request.requestNumber} 신청을 승인할까요?`)) return
  busyId.value = request.stockRequestId
  try {
    await http.patch(`/hq/stock-requests/${request.stockRequestId}/approve`)
    showToast('승인했습니다. 배송 등록을 진행하세요.')
    await refreshAll()
  } catch (e) {
    showToast(e.response?.data?.message || '승인하지 못했습니다.')
  } finally {
    busyId.value = null
  }
}

function openReject(request) {
  rejectTarget.value = request
  rejectReason.value = ''
  modalError.value = ''
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    modalError.value = '반려 사유를 입력하세요.'
    return
  }
  submitting.value = true
  modalError.value = ''
  try {
    await http.patch(`/hq/stock-requests/${rejectTarget.value.stockRequestId}/reject`,
      { rejectionReason: rejectReason.value.trim() })
    rejectTarget.value = null
    showToast('반려 처리했습니다.')
    await refreshAll()
  } catch (e) {
    modalError.value = e.response?.data?.message || '반려하지 못했습니다.'
  } finally {
    submitting.value = false
  }
}

function openShip(request) {
  shipTarget.value = request
  shipForm.value = { trackingNumber: '', courierName: '', driverName: '', estimatedArrivalAt: defaultArrival() }
  modalError.value = ''
}

/** 도착 예정은 보통 며칠 뒤라 3일 뒤 오후 2시를 기본값으로 채워 둔다. */
function defaultArrival() {
  const date = new Date(Date.now() + 3 * 86400000)
  date.setHours(14, 0, 0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function submitShip() {
  if (!shipForm.value.trackingNumber.trim()) {
    modalError.value = '운송장번호를 입력하세요.'
    return
  }
  submitting.value = true
  modalError.value = ''
  try {
    await http.patch(`/hq/stock-requests/${shipTarget.value.stockRequestId}/ship`, {
      trackingNumber: shipForm.value.trackingNumber.trim(),
      courierName: shipForm.value.courierName.trim() || null,
      driverName: shipForm.value.driverName.trim() || null,
      // datetime-local은 초가 없어서 그대로 보내면 서버의 ISO 파싱에 걸린다.
      estimatedArrivalAt: shipForm.value.estimatedArrivalAt ? `${shipForm.value.estimatedArrivalAt}:00` : null
    })
    shipTarget.value = null
    showToast('배송 등록했습니다. 지점이 수령 확인을 하면 완료됩니다.')
    await refreshAll()
  } catch (e) {
    modalError.value = e.response?.data?.message || '배송 등록하지 못했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshAll()
  loadBranches()
})
</script>

<style scoped>
*{box-sizing:border-box}
.page{min-height:100vh;color:#202938;background:#f3f6fa}
.content{margin-left:238px;padding:38px 42px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.tools{display:flex;align-items:center;gap:8px;flex-wrap:wrap}
.tools select{padding:9px 10px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:8px;font-size:11px}
.search{display:flex;align-items:center;gap:7px;width:210px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.ghost{padding:9px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:8px;font-size:11px;font-weight:800;cursor:pointer}
table{width:100%;border-collapse:collapse;font-size:11px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:top}
td strong{display:block}
.sub{display:block;margin-top:4px;color:#a3abb7;font-size:9px}
.items{display:flex;flex-wrap:wrap;gap:5px}
.item-chip{padding:5px 8px;color:#5d6675;background:#f4f6fa;border-radius:6px;font-size:10px}
.item-chip b{color:#202938}
.reason,.reject,.tracking{display:block;margin-top:6px;font-size:9px}
.reason{color:#9aa2af}.reject{color:#c63750}.tracking{color:#3169c7}
.urgency{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.urgency.LOW{color:#6b7b8c;background:#eef1f5}
.urgency.NORMAL{color:#4f7a5f;background:#eaf6ee}
.urgency.HIGH{color:#c63750;background:#ffe8ed}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.PENDING{color:#d57d00;background:#fff3d6}
.status.APPROVED,.status.PREPARING{color:#0b9654;background:#e2f8ec}
.status.SHIPPING{color:#3169c7;background:#e4f0ff}
.status.DELIVERED{color:#0b9654;background:#e2f8ec}
.status.REJECTED{color:#c63750;background:#ffe8ed}
.status.CLOSED{color:#7b838f;background:#eff1f4}
.row-actions{display:flex;gap:6px}
.row-actions button,.track{padding:7px 10px;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer;white-space:nowrap}
.row-actions button:disabled,.track:disabled{opacity:.5;cursor:wait}
.approve{color:#fff;border:0;background:#0b9654}
.reject-btn{color:#c63750;border:1px solid #ffc8d1;background:#fff4f6}
.track{color:#3169c7;border:1px solid #c7dcfa;background:#eef5ff}
.muted{color:#a8b0bb;font-size:10px}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.error-text{color:#c63750}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}

.modal-backdrop{position:fixed;inset:0;z-index:20;display:grid;padding:24px;place-items:center;background:rgb(24 30 41/45%)}
.modal{width:min(460px,100%);padding:28px;background:#fff;border-radius:16px;box-shadow:0 24px 60px rgb(20 26 38/25%)}
.modal h2{margin:0 0 6px;font-size:19px}
.current{margin:0 0 18px;color:#8c95a2;font-size:11px}
.modal label{display:grid;gap:6px;margin-top:13px;color:#5d6675;font-size:10px;font-weight:800}
.modal label em{color:#c63750;font-style:normal}
.modal input,.modal textarea{padding:11px 12px;border:1px solid #dfe3e9;border-radius:9px;outline:0;font:inherit;font-size:12px}
.modal input:focus,.modal textarea:focus{border-color:#6266f2}
.modal textarea{resize:vertical}
.two{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.form-error{margin:13px 0 0;padding:10px 12px;color:#c63750;background:#ffe8ed;border-radius:8px;font-size:11px}
.modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:20px}
.modal-actions button{padding:10px 16px;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:11px;font-weight:800;cursor:pointer}
.modal-actions .primary{color:#fff;border-color:#6266f2;background:#6266f2}
.modal-actions .danger{color:#fff;border-color:#d9455a;background:#d9455a}
.modal-actions button:disabled{opacity:.6;cursor:wait}
.toast{position:fixed;right:25px;bottom:25px;z-index:30;padding:14px 18px;color:#fff;background:#202938;border-radius:11px;box-shadow:0 12px 35px rgb(16 22 34/25%);font-size:12px;font-weight:700}

@media(max-width:980px){
  .content{margin-left:0;padding:25px 16px}
  .summary{grid-template-columns:1fr 1fr}
  table{display:block;overflow-x:auto}
  .tools{width:100%}
  .search{flex:1}
}
</style>
