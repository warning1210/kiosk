<template>
  <main class="page">
    <AdminSidebar active="deliveries" />

    <section class="content">
      <AdminPageHeader title="배송 관리" subtitle="승인된 재고를 지점으로 출고하고, 배송 진행 상황을 관리합니다." />

      <div class="summary">
        <AdminStatCard icon="📦" label="출고 대기" :value="`${summary.preparingCount}건`" delta="승인 후 출고 전" tone="orange" />
        <AdminStatCard icon="🚚" label="배송 중" :value="`${summary.shippingCount}건`" delta="지점 수령 대기" tone="blue" />
        <AdminStatCard icon="⚠" label="지연" :value="`${summary.delayedCount}건`" delta="도착 예정 초과" tone="green" />
        <AdminStatCard icon="✓" label="수령 완료" :value="`${summary.deliveredCount}건`" />
      </div>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>배송 목록</h2>
            <span>출고 대기 건은 "출고 처리"로 배송을 시작합니다. 배송번호는 자동으로 발급됩니다.</span>
          </div>
          <div class="tools">
            <select v-model="statusFilter" @change="reload">
              <option value="">전체</option>
              <option value="PREPARING">출고 대기</option>
              <option value="SHIPPING">배송 중</option>
              <option value="DELIVERED">수령 완료</option>
            </select>
            <label class="chk"><input v-model="onlyDelayed" type="checkbox" @change="reload"> 지연만</label>
            <label class="search">
              <span>⌕</span>
              <input v-model="keyword" placeholder="배송번호, 신청번호, 지점명" @keyup.enter="reload">
            </label>
            <button class="ghost" type="button" @click="reload">검색</button>
          </div>
        </div>

        <div v-if="loading" class="empty">배송 내역을 불러오는 중입니다.</div>
        <div v-else-if="error" class="empty error-text">{{ error }}</div>
        <div v-else-if="!rows.length" class="empty">조건에 맞는 배송 내역이 없습니다.</div>

        <table v-else>
          <thead>
            <tr><th>배송번호</th><th>지점</th><th>품목</th><th>배송담당자</th><th>도착 예정</th><th>상태</th><th>처리</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.stockRequestId" :class="{ 'delayed-row': row.delayed }">
              <td>
                <strong>{{ row.shipmentNumber || '출고 전' }}</strong>
                <small class="sub">{{ row.requestNumber }}</small>
              </td>
              <td>{{ row.branchName }}</td>
              <td>{{ row.menuSummary }} <b class="tubs">{{ row.totalTubs }}통</b></td>
              <td>{{ row.driverName || '-' }}</td>
              <td>
                <template v-if="row.estimatedArrivalAt">
                  {{ formatDate(row.estimatedArrivalAt) }}
                  <small v-if="row.delayed" class="delay-badge">지연</small>
                </template>
                <template v-else>-</template>
              </td>
              <td><span class="status" :class="row.requestStatus">{{ statusLabel(row.requestStatus) }}</span></td>
              <td>
                <button v-if="row.requestStatus === 'PREPARING'" class="dispatch" type="button"
                        :disabled="busyId === row.stockRequestId" @click="openDispatch(row)">
                  출고 처리
                </button>
                <span v-else-if="row.requestStatus === 'SHIPPING'" class="muted">지점 수령 대기</span>
                <span v-else class="muted">완료</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!loading && rows.length" class="pagination-foot">
          <span>전체 {{ totalElements }}건 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="totalElements" :page-size="pageSize" />
        </div>
      </section>
    </section>

    <!-- 출고 처리 -->
    <div v-if="dispatchTarget" class="modal-backdrop" @click.self="dispatchTarget = null">
      <section class="modal">
        <h2>출고 처리</h2>
        <p class="current">{{ dispatchTarget.branchName }} · {{ dispatchTarget.menuSummary }} {{ dispatchTarget.totalTubs }}통</p>

        <div class="auto-field">
          <span>배송번호</span>
          <strong>{{ previewShipmentNumber }}</strong>
          <small>출고 시 자동 발급</small>
        </div>

        <label>배송담당자 <em>필수</em><input v-model="dispatchForm.driverName" type="text" placeholder="예: 홍길동 (본사 물류팀)"></label>
        <label>도착 예정일시<input v-model="dispatchForm.estimatedArrivalAt" type="datetime-local"></label>

        <p v-if="modalError" class="form-error">{{ modalError }}</p>
        <div class="modal-actions">
          <button type="button" @click="dispatchTarget = null">취소</button>
          <button class="primary" type="button" :disabled="submitting" @click="submitDispatch">
            {{ submitting ? '처리 중...' : '출고 처리' }}
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

const rows = ref([])
const summary = ref({ preparingCount: 0, shippingCount: 0, deliveredCount: 0, delayedCount: 0 })
const loading = ref(true)
const error = ref('')
const toast = ref('')
const busyId = ref(null)

const statusFilter = ref('')
const onlyDelayed = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const totalElements = ref(0)

const dispatchTarget = ref(null)
const dispatchForm = ref({ driverName: '', estimatedArrivalAt: '' })
const submitting = ref(false)
const modalError = ref('')

const pageStart = computed(() => (totalElements.value ? (page.value - 1) * pageSize + 1 : 0))
const pageEnd = computed(() => Math.min(page.value * pageSize, totalElements.value))

// 배송번호는 서버가 실제 발급하지만, 형식을 미리 보여 주려고 화면에서도 같은 규칙으로 만든다.
const previewShipmentNumber = computed(() => {
  if (!dispatchTarget.value) return ''
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `SHIP-${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${dispatchTarget.value.stockRequestId}`
})

watch(page, load)

function statusLabel(status) {
  return ({ PREPARING: '출고 대기', SHIPPING: '배송 중', DELIVERED: '수령 완료' })[status] || status
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

/** 필터를 바꾸면 1페이지부터 다시 본다. */
function reload() {
  if (page.value === 1) load()
  else page.value = 1
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value - 1, size: pageSize }
    // "지연만"은 배송 중 + 도착 예정 초과라, 배송 중 상태로 좁혀 받은 뒤 지연 건만 남긴다.
    if (onlyDelayed.value) params.status = 'SHIPPING'
    else if (statusFilter.value) params.status = statusFilter.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const { data } = await http.get('/hq/deliveries', { params })
    let content = data.content ?? []
    if (onlyDelayed.value) content = content.filter(row => row.delayed)
    rows.value = content
    totalElements.value = onlyDelayed.value ? content.length : (data.totalElements ?? 0)
  } catch (e) {
    error.value = e.response?.data?.message || '배송 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    summary.value = (await http.get('/hq/deliveries/summary')).data
  } catch (e) {
    console.error(e)
  }
}

async function refreshAll() {
  await Promise.all([load(), loadSummary()])
}

function openDispatch(row) {
  dispatchTarget.value = row
  dispatchForm.value = { driverName: '', estimatedArrivalAt: defaultArrival() }
  modalError.value = ''
}

/** 도착 예정 기본값: 2일 뒤 오후 2시 (서버 기본값과 맞춤). */
function defaultArrival() {
  const date = new Date(Date.now() + 2 * 86400000)
  date.setHours(14, 0, 0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function submitDispatch() {
  if (!dispatchForm.value.driverName.trim()) {
    modalError.value = '배송담당자를 입력하세요.'
    return
  }
  submitting.value = true
  modalError.value = ''
  try {
    await http.patch(`/hq/deliveries/${dispatchTarget.value.stockRequestId}/dispatch`, {
      driverName: dispatchForm.value.driverName.trim(),
      // datetime-local은 초가 없어서 서버 ISO 파싱용으로 :00을 붙인다.
      estimatedArrivalAt: dispatchForm.value.estimatedArrivalAt ? `${dispatchForm.value.estimatedArrivalAt}:00` : null
    })
    dispatchTarget.value = null
    showToast('출고 처리했습니다. 지점이 수령 확인을 하면 완료됩니다.')
    await refreshAll()
  } catch (e) {
    modalError.value = e.response?.data?.message || '출고 처리하지 못했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(refreshAll)
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
.chk{display:flex;align-items:center;gap:5px;color:#4e5868;font-size:11px;font-weight:700}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.ghost{padding:9px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:8px;font-size:11px;font-weight:800;cursor:pointer}
table{width:100%;border-collapse:collapse;font-size:11px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
td strong{display:block}
.sub{display:block;margin-top:4px;color:#a3abb7;font-size:9px}
.tubs{color:#5960e9}
.delayed-row{background:#fff8f5}
.delay-badge{display:inline-block;margin-left:5px;padding:2px 6px;color:#c1601b;background:#ffe9d6;border-radius:5px;font-size:8px;font-weight:800}
.urgency{padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.PREPARING{color:#c2741d;background:#fff3d6}
.status.SHIPPING{color:#3169c7;background:#e4f0ff}
.status.DELIVERED{color:#0b9654;background:#e2f8ec}
.dispatch{padding:7px 12px;color:#fff;border:0;background:#5960e9;border-radius:7px;font-size:10px;font-weight:800;cursor:pointer;white-space:nowrap}
.dispatch:disabled{opacity:.5;cursor:wait}
.muted{color:#a8b0bb;font-size:10px}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.error-text{color:#c63750}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}

.modal-backdrop{position:fixed;inset:0;z-index:20;display:grid;padding:24px;place-items:center;background:rgb(24 30 41/45%)}
.modal{width:min(440px,100%);padding:28px;background:#fff;border-radius:16px;box-shadow:0 24px 60px rgb(20 26 38/25%)}
.modal h2{margin:0 0 6px;font-size:19px}
.current{margin:0 0 18px;color:#8c95a2;font-size:11px}
.auto-field{display:flex;align-items:center;gap:10px;padding:12px 14px;background:#f3f5fb;border-radius:10px}
.auto-field span{color:#8c95a2;font-size:10px;font-weight:800}
.auto-field strong{color:#3d43c9;font-size:13px;letter-spacing:.02em}
.auto-field small{margin-left:auto;color:#a3abb7;font-size:9px}
.modal label{display:grid;gap:6px;margin-top:13px;color:#5d6675;font-size:10px;font-weight:800}
.modal label em{color:#c63750;font-style:normal}
.modal input{padding:11px 12px;border:1px solid #dfe3e9;border-radius:9px;outline:0;font:inherit;font-size:12px}
.modal input:focus{border-color:#6266f2}
.form-error{margin:13px 0 0;padding:10px 12px;color:#c63750;background:#ffe8ed;border-radius:8px;font-size:11px}
.modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:20px}
.modal-actions button{padding:10px 16px;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:11px;font-weight:800;cursor:pointer}
.modal-actions .primary{color:#fff;border-color:#5960e9;background:#5960e9}
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
