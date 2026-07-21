<template>
  <main class="page">
    <AdminSidebar active="dashboard" />

    <section class="content">
      <AdminPageHeader title="대시보드" subtitle="아이스크림 재고 신청·배송 현황 한눈에 보기" />

      <div class="summary">
        <AdminStatCard icon="📋" label="전체 신청" :value="`${summary.totalCount}건`" />
        <AdminStatCard icon="⏱" label="처리 대기" :value="`${summary.pendingCount}건`" delta="즉시 확인 필요" tone="orange" />
        <AdminStatCard icon="🚚" label="승인 후 진행" :value="`${summary.approvedCount}건`" delta="출고 준비 · 배송" tone="blue" />
        <AdminStatCard icon="✉" label="전체 문의" :value="`${chatRoomCount}건`" delta="정상 운영" tone="green" />
      </div>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>재고 신청 현황</h2>
            <span>최근 신청 내역을 확인하고 승인/반려부터 처리하세요.</span>
          </div>
          <div class="tools">
            <select v-model="statusFilter"><option value="">전체 상태</option><option v-for="s in statuses" :key="s" :value="s">{{ statusLabel(s) }}</option></select>
            <label class="search"><span>⌕</span><input v-model="keyword" placeholder="신청번호, 분점명 검색"></label>
          </div>
        </div>

        <div v-if="!pagedRequests.length" class="empty">조건에 맞는 신청 내역이 없습니다.</div>
        <table v-else>
          <thead>
            <tr><th>신청 번호</th><th>분점명</th><th>신청 메뉴</th><th>수량</th><th>신청일</th><th>상태</th><th>처리</th></tr>
          </thead>
          <tbody>
            <tr v-for="req in pagedRequests" :key="req.stockRequestId">
              <td><strong>{{ req.requestNumber }}</strong></td>
              <td>{{ req.branchName }}</td>
              <td>{{ menuSummary(req) }}</td>
              <td>{{ totalTubs(req) }}통</td>
              <td>{{ formatDate(req.requestedAt) }}</td>
              <td><span class="status" :class="req.requestStatus">{{ statusLabel(req.requestStatus) }}</span></td>
              <td>
                <RouterLink class="detail" to="/admin/stock-requests">처리하러 가기</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredRequests.length }}건 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredRequests.length" :page-size="pageSize" />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

const statuses = ['PENDING', 'PREPARING', 'SHIPPING', 'DELIVERED', 'REJECTED']

const stockRequests = ref([])
const summary = ref({ totalCount: 0, pendingCount: 0, approvedCount: 0, rejectedCount: 0 })
const statusFilter = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 5
const chatRoomCount = ref(0)

onMounted(() => {
  loadStockRequests()
  loadSummary()
  loadChatRoomCount()
})

// 대시보드는 훑어보는 화면이라 최근 건만 받아 온다. 실제 처리는 재고 신청 화면에서 한다.
async function loadStockRequests() {
  try {
    stockRequests.value = (await http.get('/hq/stock-requests', { params: { page: 0, size: 50 } })).data.content ?? []
  } catch {
    stockRequests.value = []
  }
}

async function loadSummary() {
  try {
    summary.value = (await http.get('/hq/stock-requests/summary')).data
  } catch {
    // 요약을 못 받아도 화면 나머지는 그대로 보여 준다.
  }
}

async function loadChatRoomCount() {
  try {
    chatRoomCount.value = (await http.get('/hq/chat/rooms')).data.length
  } catch {
    chatRoomCount.value = 0
  }
}

const filteredRequests = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return stockRequests.value.filter(r =>
    (!statusFilter.value || r.requestStatus === statusFilter.value) &&
    (!word || r.requestNumber.toLowerCase().includes(word) || (r.branchName || '').toLowerCase().includes(word))
  )
})
const pageStart = computed(() => filteredRequests.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredRequests.value.length))
const pagedRequests = computed(() => filteredRequests.value.slice((page.value - 1) * pageSize, page.value * pageSize))

/** 품목이 여러 개면 "첫 맛 외 N종"으로 줄여 보여 준다. */
function menuSummary(request) {
  const items = request.items ?? []
  if (!items.length) return '-'
  return items.length === 1 ? items[0].flavorName : `${items[0].flavorName} 외 ${items.length - 1}종`
}
function totalTubs(request) {
  return (request.items ?? []).reduce((sum, item) => sum + (item.approvedQuantity ?? item.requestedQuantity ?? 0), 0)
}
function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
function statusLabel(status) {
  return ({
    PENDING: '승인 대기', APPROVED: '승인됨', PREPARING: '출고 준비',
    SHIPPING: '배송 중', DELIVERED: '수령 완료', REJECTED: '반려', CLOSED: '종료'
  })[status] || status
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.tools{display:flex;align-items:center;gap:8px}
.tools select{padding:9px 10px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:8px;font-size:11px}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
table{width:100%;border-collapse:collapse;font-size:11px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.PENDING{color:#d57d00;background:#fff3d6}.status.APPROVED{color:#0b9654;background:#e2f8ec}
.status.SHIPPING{color:#3169c7;background:#e4f0ff}.status.REJECTED{color:#c63750;background:#ffe8ed}
.status.PREPARING{color:#0b9654;background:#e2f8ec}.status.DELIVERED{color:#0b9654;background:#e2f8ec}.status.CLOSED{color:#7b838f;background:#eff1f4}
.row-actions{display:flex;gap:6px}
.row-actions button,.track,.detail{padding:7px 10px;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.approve{color:#fff;border:0;background:#0b9654}.reject{color:#c63750;border:1px solid #ffc8d1;background:#fff4f6}
.track{color:#3169c7;border:1px solid #c7dcfa;background:#eef5ff}
.detail{display:inline-block;color:#5960e9;border:1px solid #d9deea;background:#fff;text-decoration:none}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}table{display:block;overflow-x:auto}}
</style>
