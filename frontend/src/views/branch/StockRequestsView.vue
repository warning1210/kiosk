<template>
  <div class="admin-shell">
    <BranchSidebar active="stock-requests" />

    <main class="content">
      <header class="topbar">
        <div>
          <p class="eyebrow">BRANCH MANAGEMENT</p>
          <h1>입고 신청 현황</h1>
          <p>필요한 재고를 본점에 직접 신청하고, 승인·배송 상황을 확인합니다.</p>
        </div>
        <div class="top-actions">
          <button class="ghost" type="button" @click="load">새로고침</button>
          <button class="primary" type="button" @click="openCreate">+ 재고 신청</button>
        </div>
      </header>

      <section class="summary-grid">
        <article>
          <div class="summary-icon orange">⏳</div>
          <div><span>승인 대기</span><strong>{{ countOf('PENDING') }}건</strong><small>본점 확인 중</small></div>
        </article>
        <article>
          <div class="summary-icon blue">⇢</div>
          <div><span>진행 중</span><strong>{{ inProgressCount }}건</strong><small>출고 준비 · 배송 중</small></div>
        </article>
        <article>
          <div class="summary-icon green">✓</div>
          <div><span>수령 완료</span><strong>{{ countOf('DELIVERED') }}건</strong><small>재고 반영 대기 포함</small></div>
        </article>
        <article>
          <div class="summary-icon red">✕</div>
          <div><span>반려</span><strong>{{ countOf('REJECTED') }}건</strong><small>사유 확인 후 재신청</small></div>
        </article>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="tabs">
            <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
              {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
            </button>
          </div>
        </div>

        <div v-if="loading" class="empty">신청 내역을 불러오는 중입니다.</div>
        <div v-else-if="error" class="empty error-text">{{ error }}</div>
        <div v-else-if="!filteredRequests.length" class="empty">
          해당하는 신청 내역이 없습니다.
          <button class="link" type="button" @click="openCreate">지금 재고 신청하기</button>
        </div>

        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr><th>신청번호</th><th>신청 품목</th><th>긴급도</th><th>상태</th><th>신청일</th><th>처리</th></tr>
            </thead>
            <tbody>
              <tr v-for="request in filteredRequests" :key="request.stockRequestId">
                <td>
                  <strong class="req-no">{{ request.requestNumber }}</strong>
                  <span class="sub">{{ request.requesterAdminName }}</span>
                </td>
                <td>
                  <div class="items">
                    <span v-for="item in request.items" :key="item.flavorId" class="item-chip">
                      {{ item.flavorName }}
                      <b>{{ item.approvedQuantity ?? item.requestedQuantity }}통</b>
                    </span>
                  </div>
                  <small v-if="request.requestReason" class="reason">사유 · {{ request.requestReason }}</small>
                  <small v-if="request.rejectionReason" class="reject">반려 사유 · {{ request.rejectionReason }}</small>
                  <small v-if="request.trackingNumber" class="tracking">
                    운송장 {{ request.trackingNumber }}<template v-if="request.courierName"> · {{ request.courierName }}</template>
                  </small>
                </td>
                <td><span class="urgency" :class="request.urgency.toLowerCase()">{{ urgencyLabel(request.urgency) }}</span></td>
                <td><span class="status" :class="statusOf(request.requestStatus).key"><i></i>{{ statusOf(request.requestStatus).label }}</span></td>
                <td>
                  <span class="date">{{ formatDate(request.requestedAt) }}</span>
                  <small v-if="request.estimatedArrivalAt" class="sub">도착예정 {{ formatDate(request.estimatedArrivalAt) }}</small>
                </td>
                <td>
                  <div class="row-actions">
                    <button v-if="request.requestStatus === 'PENDING'" class="cancel" type="button"
                            :disabled="busyId === request.stockRequestId" @click="cancelRequest(request)">
                      신청 취소
                    </button>
                    <button v-else-if="request.requestStatus === 'SHIPPING'" class="receive" type="button"
                            :disabled="busyId === request.stockRequestId" @click="confirmReceipt(request)">
                      수령 확인
                    </button>
                    <span v-else-if="request.requestStatus === 'DELIVERED'" class="muted">재고 화면에서 입고 처리</span>
                    <span v-else class="muted">-</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <!-- 재고 신청 등록 -->
    <div v-if="createOpen" class="modal-backdrop" @click.self="closeCreate">
      <section class="modal">
        <button class="close" type="button" @click="closeCreate">×</button>
        <p class="eyebrow">STOCK REQUEST</p>
        <h2>재고 신청</h2>
        <p class="current">필요한 맛을 골라 수량(통)을 입력하세요. 1통은 3,000g입니다.</p>

        <label class="search-label">
          맛 검색
          <input v-model="flavorKeyword" type="text" placeholder="예: 요거트, 초콜릿">
        </label>

        <div class="flavor-picker">
          <button v-for="flavor in pickableFlavors" :key="flavor.flavorId" type="button" @click="addItem(flavor)">
            + {{ flavor.flavorName }}
          </button>
          <p v-if="!pickableFlavors.length" class="muted small">검색 결과가 없거나 이미 모두 담았습니다.</p>
        </div>

        <div v-if="form.items.length" class="picked">
          <div v-for="(item, index) in form.items" :key="item.flavorId" class="picked-row">
            <span class="picked-name">{{ item.flavorName }}</span>
            <div class="qty">
              <button type="button" @click="item.requestedQuantity = Math.max(1, item.requestedQuantity - 1)">−</button>
              <input v-model.number="item.requestedQuantity" min="1" type="number">
              <button type="button" @click="item.requestedQuantity++">+</button>
              <span class="unit">통</span>
            </div>
            <button class="remove" type="button" @click="form.items.splice(index, 1)">삭제</button>
          </div>
        </div>
        <p v-else class="muted small picked-empty">아직 담은 품목이 없습니다.</p>

        <label>
          긴급도
          <div class="type-buttons">
            <button v-for="option in urgencyOptions" :key="option.value"
                    :class="{ active: form.urgency === option.value }" type="button"
                    @click="form.urgency = option.value">
              {{ option.label }}
            </button>
          </div>
        </label>

        <label>신청 사유<input v-model="form.requestReason" type="text" placeholder="예: 주말 행사 대비 물량 확보"></label>

        <p v-if="formError" class="form-error">{{ formError }}</p>

        <div class="modal-actions">
          <button type="button" @click="closeCreate">취소</button>
          <button class="primary" type="button" :disabled="submitting" @click="submitRequest">
            {{ submitting ? '신청 중...' : '신청하기' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import branchApi from '../../api/branch'
import publicApi from '../../api/http'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

// 진행 중으로 묶어서 보여 줄 상태들 - 본점이 받아서 실제로 처리하고 있는 단계
const IN_PROGRESS = ['APPROVED', 'PREPARING', 'SHIPPING']

const requests = ref([])
const flavors = ref([])
const loading = ref(true)
const error = ref('')
const filter = ref('all')
const busyId = ref(null)
const toast = ref('')

const createOpen = ref(false)
const submitting = ref(false)
const formError = ref('')
const flavorKeyword = ref('')
const form = reactive({ urgency: 'NORMAL', requestReason: '', items: [] })

const tabs = [
  { label: '전체', value: 'all' },
  { label: '승인 대기', value: 'PENDING' },
  { label: '진행 중', value: 'progress' },
  { label: '수령 완료', value: 'DELIVERED' },
  { label: '반려', value: 'REJECTED' }
]
const urgencyOptions = [
  { label: '여유', value: 'LOW' },
  { label: '보통', value: 'NORMAL' },
  { label: '긴급', value: 'HIGH' }
]

const inProgressCount = computed(() => requests.value.filter(r => IN_PROGRESS.includes(r.requestStatus)).length)

const filteredRequests = computed(() => {
  if (filter.value === 'all') return requests.value
  if (filter.value === 'progress') return requests.value.filter(r => IN_PROGRESS.includes(r.requestStatus))
  return requests.value.filter(r => r.requestStatus === filter.value)
})

// 이미 담은 맛은 중복 신청이 서버에서 막히므로 목록에서 빼 준다.
const pickableFlavors = computed(() => {
  const picked = new Set(form.items.map(item => item.flavorId))
  const keyword = flavorKeyword.value.trim().toLowerCase()
  return flavors.value
    .filter(flavor => !picked.has(flavor.flavorId))
    .filter(flavor => !keyword || flavor.flavorName.toLowerCase().includes(keyword))
    .slice(0, 12)
})

function countOf(status) {
  return requests.value.filter(r => r.requestStatus === status).length
}
function tabCount(value) {
  if (value === 'all') return requests.value.length
  if (value === 'progress') return inProgressCount.value
  return countOf(value)
}
function statusOf(status) {
  return ({
    PENDING: { key: 'pending', label: '승인 대기' },
    APPROVED: { key: 'progress', label: '승인됨' },
    PREPARING: { key: 'progress', label: '출고 준비' },
    SHIPPING: { key: 'progress', label: '배송 중' },
    DELIVERED: { key: 'done', label: '수령 완료' },
    REJECTED: { key: 'rejected', label: '반려' },
    CLOSED: { key: 'closed', label: '종료' }
  })[status] || { key: 'closed', label: status }
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
  showToast.timer = window.setTimeout(() => { toast.value = '' }, 2600)
}

async function load() {
  error.value = ''
  try {
    // size를 넉넉히 잡아 한 화면에서 다 보이게 한다. 건수가 많아지면 페이지 UI를 붙이면 된다.
    const { data } = await branchApi.get('/stock-requests', { params: { page: 0, size: 100 } })
    requests.value = data.content ?? []
  } catch (e) {
    error.value = e.response?.data?.message || '신청 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadFlavors() {
  try {
    // 맛 목록은 로그인 없이도 볼 수 있는 공개 API라 http 인스턴스를 쓴다.
    const { data } = await publicApi.get('/flavors')
    flavors.value = data
  } catch (e) {
    console.error(e)
  }
}

function openCreate() {
  form.urgency = 'NORMAL'
  form.requestReason = ''
  form.items = []
  flavorKeyword.value = ''
  formError.value = ''
  createOpen.value = true
}
function closeCreate() { createOpen.value = false }

function addItem(flavor) {
  form.items.push({ flavorId: flavor.flavorId, flavorName: flavor.flavorName, requestedQuantity: 1 })
  flavorKeyword.value = ''
}

async function submitRequest() {
  formError.value = ''
  if (!form.items.length) {
    formError.value = '신청할 맛을 1개 이상 선택하세요.'
    return
  }
  if (form.items.some(item => !item.requestedQuantity || item.requestedQuantity < 1)) {
    formError.value = '신청 수량은 1통 이상이어야 합니다.'
    return
  }
  submitting.value = true
  try {
    await branchApi.post('/stock-requests', {
      requestReason: form.requestReason,
      urgency: form.urgency,
      items: form.items.map(item => ({ flavorId: item.flavorId, requestedQuantity: item.requestedQuantity }))
    })
    closeCreate()
    showToast('재고 신청을 등록했습니다.')
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || '재고 신청에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

async function cancelRequest(request) {
  if (!window.confirm(`${request.requestNumber} 신청을 취소할까요?`)) return
  busyId.value = request.stockRequestId
  try {
    await branchApi.patch(`/stock-requests/${request.stockRequestId}/cancel`)
    showToast('신청을 취소했습니다.')
    await load()
  } catch (e) {
    showToast(e.response?.data?.message || '취소하지 못했습니다.')
  } finally {
    busyId.value = null
  }
}

async function confirmReceipt(request) {
  busyId.value = request.stockRequestId
  try {
    await branchApi.patch(`/stock-requests/${request.stockRequestId}/confirm-receipt`)
    showToast('수령을 확인했습니다. 재고 화면에서 입고 처리하면 재고에 반영됩니다.')
    await load()
  } catch (e) {
    showToast(e.response?.data?.message || '수령 확인에 실패했습니다.')
  } finally {
    busyId.value = null
  }
}

onMounted(() => {
  load()
  loadFlavors()
})
</script>

<style scoped>
:global(body) { background: #f7f7fa; color: #252329; }
button, input { font: inherit; }
button { cursor: pointer; }
.admin-shell { min-height: 100vh; }
.content { margin-left: 238px; padding: 38px 44px 60px; }
.topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 27px; }
.eyebrow { margin: 0 0 7px !important; color: #ed3c8d !important; font-size: 10px !important; font-weight: 900; letter-spacing: .14em; }
.topbar h1 { margin: 0 0 7px; font-size: 30px; letter-spacing: -.05em; }
.topbar p { margin: 0; color: #8b8389; font-size: 14px; }
.top-actions { display: flex; align-items: center; gap: 8px; }
.primary { padding: 12px 18px; color: #fff; border: 0; background: #ef3f91; border-radius: 10px; font-size: 13px; font-weight: 800; box-shadow: 0 8px 20px rgb(239 63 145 / 20%); }
.primary:disabled { opacity: .55; cursor: wait; }
.ghost { padding: 11px 15px; color: #6d656b; border: 1px solid #e2dce0; background: #fff; border-radius: 10px; font-size: 12px; font-weight: 700; }

.summary-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 14px; margin-bottom: 20px; }
.summary-grid article { display: flex; align-items: center; gap: 15px; padding: 20px; border: 1px solid #ece8eb; background: #fff; border-radius: 15px; box-shadow: 0 4px 14px rgb(55 39 47 / 3%); }
.summary-icon { display: grid; flex: 0 0 43px; height: 43px; place-items: center; border-radius: 12px; font-weight: 900; }
.summary-icon.orange { color: #e7892b; background: #fff5e9; }
.summary-icon.blue { color: #4384db; background: #edf5ff; }
.summary-icon.green { color: #34a56f; background: #edf9f3; }
.summary-icon.red { color: #d9455a; background: #ffeef0; }
.summary-grid span, .summary-grid strong, .summary-grid small { display: block; }
.summary-grid span { color: #8e868b; font-size: 11px; }
.summary-grid strong { margin: 4px 0 3px; font-size: 21px; }
.summary-grid small { color: #aaa2a7; font-size: 10px; }

.panel { overflow: hidden; border: 1px solid #ebe6e9; background: #fff; border-radius: 16px; box-shadow: 0 6px 20px rgb(55 39 47 / 4%); }
.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 19px; border-bottom: 1px solid #eee9ec; }
.tabs { display: flex; gap: 3px; }
.tabs button { padding: 8px 11px; color: #8a8287; border: 0; background: transparent; border-radius: 8px; font-size: 12px; font-weight: 700; }
.tabs button span { padding: 2px 5px; background: #f1eef0; border-radius: 6px; font-size: 9px; }
.tabs button.active { color: #e93685; background: #fff0f6; }
.tabs button.active span { background: #ffdce9; }

.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th { padding: 12px 16px; color: #9d969b; background: #fbfafb; font-size: 10px; font-weight: 700; text-align: left; }
td { padding: 14px 16px; border-top: 1px solid #f1edf0; font-size: 12px; vertical-align: top; }
tbody tr:hover { background: #fffbfd; }
.req-no { display: block; font-size: 12px; }
.sub { display: block; margin-top: 4px; color: #aaa2a7; font-size: 9px; }
.date { font-size: 11px; }
.items { display: flex; flex-wrap: wrap; gap: 5px; }
.item-chip { padding: 5px 8px; color: #6a6267; background: #f6f3f5; border-radius: 7px; font-size: 10px; }
.item-chip b { margin-left: 4px; color: #29252a; }
.reason, .reject, .tracking { display: block; margin-top: 6px; font-size: 9px; }
.reason { color: #a49ca1; }
.reject { color: #c74454; }
.tracking { color: #397bc7; }
.urgency { padding: 5px 8px; border-radius: 7px; font-size: 10px; font-weight: 800; }
.urgency.low { color: #6b7b8c; background: #f0f3f6; }
.urgency.normal { color: #4f7a5f; background: #eef7f1; }
.urgency.high { color: #c74454; background: #ffeaed; }
.status { display: inline-flex; align-items: center; gap: 6px; padding: 5px 8px; border-radius: 7px; font-size: 10px; font-weight: 800; }
.status i { width: 5px; height: 5px; border-radius: 50%; }
.status.pending { color: #c2741d; background: #fff3e2; } .status.pending i { background: #e79635; }
.status.progress { color: #2f6db8; background: #eaf2fd; } .status.progress i { background: #4384db; }
.status.done { color: #268b5d; background: #eaf8f1; } .status.done i { background: #39a970; }
.status.rejected { color: #c74454; background: #ffeaed; } .status.rejected i { background: #df4d5e; }
.status.closed { color: #8a8287; background: #f2eff1; } .status.closed i { background: #aaa2a7; }
.row-actions { display: flex; gap: 5px; }
.row-actions button { white-space: nowrap; padding: 7px 9px; border: 1px solid #dfd9dd; background: #fff; border-radius: 7px; font-size: 9px; font-weight: 700; }
.row-actions button:disabled { opacity: .5; cursor: wait; }
.row-actions .cancel { color: #c74454; border-color: #f3c2c9; }
.row-actions .receive { color: #fff; border-color: #397bc7; background: #397bc7; }
.muted { color: #b0a9ad; font-size: 10px; }
.small { font-size: 10px; }
.empty { padding: 70px; color: #999197; text-align: center; }
.error-text { color: #c74454; }
.link { margin-left: 8px; color: #e93685; border: 0; background: transparent; font-size: 12px; font-weight: 800; text-decoration: underline; }

.modal-backdrop { position: fixed; inset: 0; z-index: 20; display: grid; padding: 24px; place-items: center; background: rgb(28 22 25 / 46%); backdrop-filter: blur(3px); }
.modal { position: relative; width: min(520px,100%); max-height: 88vh; overflow-y: auto; padding: 30px; background: #fff; border-radius: 19px; box-shadow: 0 25px 70px rgb(32 17 24 / 25%); }
.close { position: absolute; top: 15px; right: 16px; border: 0; background: transparent; font-size: 25px; }
.modal h2 { margin: 0 0 8px; font-size: 22px; }
.current { margin: 0 0 20px; color: #898187; font-size: 12px; }
.modal label { display: grid; gap: 7px; margin-top: 14px; color: #665f63; font-size: 11px; font-weight: 800; }
.modal input { padding: 11px 12px; border: 1px solid #dfd9dd; border-radius: 9px; outline: 0; }
.modal input:focus { border-color: #ef6ba7; }
.search-label { margin-top: 0 !important; }
.flavor-picker { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 9px; }
.flavor-picker button { padding: 7px 9px; color: #756d72; border: 1px solid #e5dfe3; background: #faf9fa; border-radius: 7px; font-size: 10px; }
.flavor-picker button:hover { color: #e93685; border-color: #f4b8d3; background: #fff5f9; }
.picked { display: grid; gap: 6px; margin-top: 14px; padding: 12px; background: #faf7f9; border-radius: 11px; }
.picked-row { display: flex; align-items: center; gap: 8px; }
.picked-name { flex: 1; font-size: 11px; font-weight: 700; }
.qty { display: flex; align-items: center; gap: 4px; }
.qty button { width: 26px; height: 26px; color: #6d656b; border: 1px solid #e0dade; background: #fff; border-radius: 7px; font-size: 13px; }
.qty input { width: 48px; padding: 5px; border: 1px solid #e0dade; border-radius: 7px; font-size: 11px; text-align: center; }
.qty .unit { color: #918990; font-size: 10px; }
.remove { padding: 5px 7px; color: #b56070; border: 1px solid #f0d5da; background: #fff; border-radius: 7px; font-size: 9px; }
.picked-empty { margin-top: 12px; }
.type-buttons { display: grid; grid-template-columns: repeat(3,1fr); gap: 7px; }
.type-buttons button { padding: 10px; color: #746c71; border: 1px solid #e3dde1; background: #fff; border-radius: 9px; font-size: 11px; font-weight: 700; }
.type-buttons button.active { color: #e93685; border-color: #ef82b3; background: #fff2f7; }
.form-error { margin: 14px 0 0; padding: 10px 12px; color: #c52f47; background: #ffecef; border-radius: 8px; font-size: 11px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 7px; margin-top: 19px; }
.modal-actions button { padding: 10px 16px; border: 1px solid #ded7dc; background: #fff; border-radius: 9px; font-size: 11px; font-weight: 800; }
.modal-actions .primary { color: #fff; border-color: #ef3f91; background: #ef3f91; }
.toast { position: fixed; right: 25px; bottom: 25px; z-index: 30; padding: 14px 18px; color: #fff; background: #272329; border-radius: 11px; box-shadow: 0 12px 35px rgb(24 16 20 / 25%); font-size: 12px; font-weight: 700; }

@media (max-width: 1100px) { .content { margin-left: 190px; padding: 30px 24px; } .summary-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 760px) { .content { margin-left: 0; padding: 22px 14px; } .topbar { align-items: flex-start; flex-direction: column; gap: 14px; } .summary-grid { grid-template-columns: 1fr 1fr; } .tabs { overflow-x: auto; width: 100%; } }
</style>
