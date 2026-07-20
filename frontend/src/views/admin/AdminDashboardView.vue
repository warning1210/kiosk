<template>
  <main class="page">
    <AdminSidebar active="dashboard" />

    <section class="content">
      <header>
        <div>
          <p>본점 관리</p>
          <h1>지점 현황 대시보드</h1>
          <span>전 지점의 운영 상태와 매출을 한눈에 확인하세요.</span>
        </div>
        <button class="refresh" type="button" @click="load">새로고침</button>
      </header>

      <div v-if="error" class="alert">{{ error }}</div>

      <div class="summary">
        <article><span>전체 지점</span><b>{{ branches.length }}</b></article>
        <article><span>영업중</span><b>{{ activeCount }}</b></article>
        <article><span>오늘 총매출</span><b>₩{{ totalToday.toLocaleString() }}</b></article>
        <article><span>이번달 총매출</span><b>₩{{ totalMonth.toLocaleString() }}</b></article>
      </div>

      <section class="list-card">
        <div class="list-head">
          <h2>지점 목록</h2>
          <span>{{ loading ? '불러오는 중' : `${branches.length}개 지점` }}</span>
        </div>

        <div v-if="!loading && !branches.length" class="empty">등록된 지점이 없습니다.</div>
        <table v-else class="branch-table">
          <thead>
            <tr>
              <th>지점명</th><th>운영상태</th><th>키오스크</th><th>혼잡도</th>
              <th>일 매출</th><th>월 매출</th><th>년 매출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="branch in branches" :key="branch.branchId">
              <td><strong>{{ branch.branchName }}</strong><small>{{ branch.region || branch.address }}</small></td>
              <td><span :class="['status', operationClass(branch.operationStatus)]">{{ operationLabel(branch.operationStatus) }}</span></td>
              <td><span :class="['status', kioskClass(branch.kioskStatus)]">{{ branch.kioskStatus }}</span></td>
              <td>{{ branch.isBusy ? `혼잡 (${branch.estimatedWaitMinutes ?? '-'}분 대기)` : '원활' }}</td>
              <td>₩{{ branch.salesToday.toLocaleString() }}</td>
              <td>₩{{ branch.salesMonth.toLocaleString() }}</td>
              <td>₩{{ branch.salesYear.toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'

const branches = ref([])
const loading = ref(true)
const error = ref('')

const activeCount = computed(() => branches.value.filter((b) => b.operationStatus === 'ACTIVE').length)
const totalToday = computed(() => branches.value.reduce((sum, b) => sum + b.salesToday, 0))
const totalMonth = computed(() => branches.value.reduce((sum, b) => sum + b.salesMonth, 0))

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

function operationLabel(status) {
  return { ACTIVE: '영업중', PENDING: '오픈 준비', SUSPENDED: '영업 중지', CLOSED: '폐점' }[status] || status
}
function operationClass(status) {
  return { ACTIVE: 'approved', PENDING: 'pending', SUSPENDED: 'rejected', CLOSED: 'rejected' }[status] || 'pending'
}
function kioskClass(status) {
  return { ACTIVE: 'approved', MAINTENANCE: 'pending', INACTIVE: 'rejected' }[status] || 'pending'
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}header{display:flex;align-items:flex-end;justify-content:space-between}header p{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}h1{margin:0;font-size:27px}header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}.refresh{padding:10px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:9px;font-weight:800;cursor:pointer}
.alert{margin-top:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:20px 0}.summary article{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.summary span{color:#798392;font-size:11px}.summary b{font-size:22px}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.list-head{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #e9edf2}.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.branch-table{width:100%;border-collapse:collapse;font-size:11px}.branch-table th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}.branch-table td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}.branch-table td strong{display:block;font-size:12px}.branch-table td small{display:block;margin-top:3px;color:#98a1ae;font-size:9px}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}.status.rejected{color:#c63750;background:#ffe8ed}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.branch-table{display:block;overflow-x:auto}}
</style>
