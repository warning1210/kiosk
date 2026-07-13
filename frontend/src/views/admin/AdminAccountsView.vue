<template>
  <main class="page">
    <aside>
      <div class="brand">♙　배스킨라빈스 <span>본점</span></div>
      <nav><b>계정 관리</b></nav>
    </aside>

    <section class="content">
      <header>
        <div>
          <p>본점 관리</p>
          <h1>지점 계정 관리</h1>
          <span>지점 개설 신청을 확인하고 수락하세요.</span>
        </div>
        <button class="refresh" type="button" @click="load">새로고침</button>
      </header>

      <div class="summary">
        <article><span>승인 대기</span><b>{{ pendingCount }}</b></article>
        <article><span>전체 신청</span><b>{{ applications.length }}</b></article>
      </div>

      <div v-if="error" class="alert">{{ error }}</div>

      <section class="list-card">
        <div class="list-head">
          <h2>지점 개설 신청</h2>
          <span>{{ loading ? '불러오는 중' : `${applications.length}건` }}</span>
        </div>

        <div v-if="!loading && !applications.length" class="empty">접수된 지점 신청이 없습니다.</div>
        <article v-for="application in applications" :key="application.applicationId" class="application">
          <div class="identity">
            <span class="avatar">{{ application.branchName.slice(0, 1) }}</span>
            <div><strong>{{ application.branchName }}</strong><p>{{ application.managerName }} 지점장</p></div>
          </div>
          <dl>
            <div><dt>주소</dt><dd>{{ application.address }}</dd></div>
            <div><dt>연락처</dt><dd>{{ application.phone }}</dd></div>
            <div><dt>이메일</dt><dd>{{ application.email }}</dd></div>
            <div><dt>사업자번호</dt><dd>{{ application.businessNumber }}</dd></div>
            <div><dt>로그인 아이디</dt><dd>{{ application.loginId || '기존 신청 · 계정 정보 없음' }}</dd></div>
          </dl>
          <div class="request-state">
            <span :class="['status', application.status.toLowerCase()]">{{ statusLabel(application.status) }}</span>
            <small>{{ formatDate(application.appliedAt) }}</small>
          </div>
          <div class="actions">
            <button v-if="application.status === 'PENDING' && application.loginId" :disabled="approvingId === application.applicationId" type="button" @click="approve(application)">
              {{ approvingId === application.applicationId ? '처리 중' : '수락' }}
            </button>
            <span v-else-if="application.status === 'PENDING'" class="resubmit">재신청 필요</span>
            <button v-else-if="application.accountStatus === 'ACTIVE'" class="delete" :disabled="deletingId === application.applicationId" type="button" @click="deleteAccount(application)">
              {{ deletingId === application.applicationId ? '삭제 중' : '계정 삭제' }}
            </button>
            <span v-else-if="application.accountStatus === 'DELETED'" class="deleted">삭제된 계정</span>
            <span v-else class="joined">가입 대기</span>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/http'

const applications = ref([])
const loading = ref(true)
const error = ref('')
const approvingId = ref(null)
const deletingId = ref(null)
const pendingCount = computed(() => applications.value.filter(item => item.status === 'PENDING').length)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    applications.value = (await http.get('/hq/branch-applications')).data
  } catch {
    error.value = '지점 신청 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function approve(application) {
  approvingId.value = application.applicationId
  error.value = ''
  try {
    const { data } = await http.post(`/hq/branch-applications/${application.applicationId}/approve`)
    Object.assign(application, data)
  } catch (e) {
    error.value = e.response?.data?.message || '신청을 수락하지 못했습니다.'
  } finally {
    approvingId.value = null
  }
}

async function deleteAccount(application) {
  if (!window.confirm(`${application.branchName}의 로그인 계정을 삭제할까요?\n주문·매출·재고 이력은 보존됩니다.`)) return
  deletingId.value = application.applicationId
  error.value = ''
  try {
    const { data } = await http.delete(`/hq/branch-applications/${application.applicationId}/account`)
    Object.assign(application, data)
  } catch (e) {
    error.value = e.response?.data?.message || '지점 계정을 삭제하지 못했습니다.'
  } finally {
    deletingId.value = null
  }
}

function statusLabel(status) {
  return ({ PENDING: '승인 대기', APPROVED: '수락 완료', REJECTED: '반려' })[status] || status
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
*{box-sizing:border-box}.page{display:grid;min-height:100vh;grid-template-columns:230px 1fr;color:#202938;background:#f3f6fa}aside{padding:30px 22px;color:#fff;background:linear-gradient(165deg,#6165ee,#8e59df 60%,#e83e9b)}.brand{font-size:15px;font-weight:900}.brand span{display:block;margin:5px 0 40px;font-size:10px;font-weight:600;opacity:.7}nav b{display:block;padding:13px 14px;color:#6165ee;background:#fff;border-radius:10px;font-size:12px}.content{padding:38px 42px}header{display:flex;align-items:flex-end;justify-content:space-between}header p{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}h1{margin:0;font-size:27px}header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}.refresh{padding:10px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:9px;font-weight:800;cursor:pointer}.summary{display:grid;grid-template-columns:repeat(2,minmax(180px,250px));gap:14px;margin:26px 0}.summary article{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.summary span{color:#798392;font-size:11px}.summary b{font-size:25px}.alert{margin-bottom:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.list-head{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #e9edf2}.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}.application{display:grid;grid-template-columns:190px minmax(330px,1fr) 105px 120px;gap:20px;align-items:center;padding:19px 22px;border-bottom:1px solid #eef1f5}.application:last-child{border:0}.identity{display:flex;align-items:center;gap:11px}.avatar{display:grid;width:40px;height:40px;flex:0 0 auto;place-items:center;color:#fff;background:linear-gradient(145deg,#ee4b9d,#6568ee);border-radius:12px;font-weight:900}.identity strong{font-size:12px}.identity p{margin:4px 0 0;color:#88919e;font-size:10px}dl{display:grid;grid-template-columns:1.4fr .8fr;gap:8px 18px;margin:0}dl div{min-width:0}dt{color:#959daa;font-size:8px}dd{overflow:hidden;margin:3px 0 0;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.request-state{display:grid;gap:7px}.status{width:max-content;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}.status.rejected{color:#c63750;background:#ffe8ed}.request-state small{color:#9aa2ad;font-size:8px}.actions button{width:100%;padding:10px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-size:10px;font-weight:900;cursor:pointer}.actions button:disabled{opacity:.55}.actions .copy{color:#5d62e8;border:1px solid #cfd3fa;background:#f5f6ff}.joined{color:#8e97a4;font-size:10px}.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}@media(max-width:980px){.page{grid-template-columns:1fr}aside{display:none}.content{padding:25px 16px}.application{grid-template-columns:1fr}.summary{grid-template-columns:1fr 1fr}dl{grid-template-columns:1fr 1fr}}
.joined{color:#168c50;font-weight:800}.resubmit{color:#c63750;font-size:9px;font-weight:800}
.actions .delete{color:#d13852;border:1px solid #ffc8d1;background:#fff4f6}.deleted{color:#9aa2ad;font-size:9px;font-weight:800}
</style>
