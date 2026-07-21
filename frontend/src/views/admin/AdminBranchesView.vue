<template>
  <main class="page">
    <AdminSidebar active="branches" />

    <section class="content">
      <AdminPageHeader title="분점 관리" subtitle="전국 지점의 운영 상태와 매출 현황을 확인하고 관리할 수 있습니다." />

      <div v-if="error" class="alert">{{ error }}</div>

      <div class="summary">
        <AdminStatCard icon="⌂" label="전체 분점" :value="`${branches.length}개`" />
        <AdminStatCard icon="✓" label="정상 운영" :value="`${normalCount}개`" tone="green" />
        <AdminStatCard icon="!" label="주의 운영" :value="`${busyCount}개`" tone="orange" />
        <AdminStatCard icon="⏸" label="일시 휴점" :value="`${suspendedCount}개`" tone="pink" />
      </div>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>지점 목록</h2>
            <span>{{ loading ? '불러오는 중' : `${filteredBranches.length}개 지점` }}</span>
          </div>
          <div class="tools">
            <RouterLink class="invite-link" to="/admin/branch-invites">지점 초대 관리</RouterLink>
            <label class="search"><span>⌕</span><input v-model="keyword" placeholder="지점명, 주소 검색"></label>
          </div>
        </div>

        <div class="tabs">
          <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
            {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!loading && !pagedBranches.length" class="empty">조건에 맞는 지점이 없습니다.</div>
        <table v-else>
          <thead><tr><th>지점명</th><th>주소</th><th>운영상태</th><th>키오스크</th><th>이번달 매출</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="branch in pagedBranches" :key="branch.branchId">
              <td><strong>{{ branch.branchName }}</strong><small>{{ branch.region || '-' }}</small></td>
              <td>{{ branch.address || '-' }}</td>
              <td><span class="status" :class="statusOf(branch).key">{{ statusOf(branch).label }}</span></td>
              <td><span class="status" :class="kioskClass(branch.kioskStatus)">{{ branch.kioskStatus }}</span></td>
              <td>₩{{ branch.salesMonth.toLocaleString() }}</td>
              <td><button class="detail" type="button" @click="expanded = expanded === branch.branchId ? null : branch.branchId">{{ expanded === branch.branchId ? '접기' : '상세보기' }}</button></td>
            </tr>
            <tr v-for="branch in pagedBranches.filter(b => expanded === b.branchId)" :key="`d-${branch.branchId}`" class="detail-row">
              <td colspan="6">
                <div class="detail-grid">
                  <div><span>일 매출</span><strong>₩{{ branch.salesToday.toLocaleString() }}</strong></div>
                  <div><span>월 매출</span><strong>₩{{ branch.salesMonth.toLocaleString() }}</strong></div>
                  <div><span>연 매출</span><strong>₩{{ branch.salesYear.toLocaleString() }}</strong></div>
                  <div><span>혼잡도</span><strong>{{ branch.isBusy ? `혼잡 (${branch.estimatedWaitMinutes ?? '-'}분 대기)` : '원활' }}</strong></div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredBranches.length }}개 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredBranches.length" :page-size="pageSize" />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

const branches = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const filter = ref('all')
const page = ref(1)
const pageSize = 6
const expanded = ref(null)

const tabs = [
  { label: '전체', value: 'all' }, { label: '정상 운영', value: 'normal' },
  { label: '주의 운영', value: 'busy' }, { label: '일시 휴점', value: 'suspended' }
]

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    branches.value = (await http.get('/hq/branches')).data
  } catch (e) {
    error.value = e.response?.data?.message || '지점 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function bucketOf(branch) {
  if (branch.operationStatus === 'SUSPENDED' || branch.operationStatus === 'CLOSED') return 'suspended'
  if (branch.operationStatus === 'ACTIVE' && branch.isBusy) return 'busy'
  if (branch.operationStatus === 'ACTIVE') return 'normal'
  return 'other'
}

const normalCount = computed(() => branches.value.filter(b => bucketOf(b) === 'normal').length)
const busyCount = computed(() => branches.value.filter(b => bucketOf(b) === 'busy').length)
const suspendedCount = computed(() => branches.value.filter(b => bucketOf(b) === 'suspended').length)

function tabCount(value) {
  if (value === 'all') return branches.value.length
  return branches.value.filter(b => bucketOf(b) === value).length
}

const filteredBranches = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return branches.value.filter(b =>
    (filter.value === 'all' || bucketOf(b) === filter.value) &&
    (!word || b.branchName.toLowerCase().includes(word) || (b.address || '').toLowerCase().includes(word))
  )
})
const pageStart = computed(() => filteredBranches.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredBranches.value.length))
const pagedBranches = computed(() => filteredBranches.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function statusOf(branch) {
  const bucket = bucketOf(branch)
  return {
    normal: { key: 'approved', label: '정상 운영' },
    busy: { key: 'pending', label: '주의 운영' },
    suspended: { key: 'rejected', label: '일시 휴점' },
    other: { key: 'pending', label: branch.operationStatus }
  }[bucket]
}
function kioskClass(status) {
  return { ACTIVE: 'approved', MAINTENANCE: 'pending', INACTIVE: 'rejected' }[status] || 'pending'
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.alert{margin-top:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.tools{display:flex;align-items:center;gap:10px}
.invite-link{padding:9px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:8px;font-size:11px;font-weight:800;text-decoration:none;white-space:nowrap}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.tabs{display:flex;gap:3px;padding:12px 22px 0}
.tabs button{padding:8px 11px;color:#697487;border:0;background:transparent;border-radius:8px;font-size:12px;font-weight:700;cursor:pointer}
.tabs button span{padding:2px 5px;background:#eef0f3;border-radius:6px;font-size:9px}
.tabs button.active{color:#5f63ee;background:#eef0ff}.tabs button.active span{background:#dbdefc}
table{width:100%;border-collapse:collapse;font-size:11px;margin-top:8px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
td strong{display:block;font-size:12px}td small{display:block;margin-top:3px;color:#98a1ae;font-size:9px}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}.status.rejected{color:#c63750;background:#ffe8ed}
.detail{padding:7px 10px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.detail-row td{background:#f8f9fc}
.detail-grid{display:grid;grid-template-columns:repeat(4,minmax(120px,1fr));gap:14px}
.detail-grid span{display:block;color:#8c95a2;font-size:9px}
.detail-grid strong{display:block;margin-top:5px;font-size:12px}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}table{display:block;overflow-x:auto}}
</style>
