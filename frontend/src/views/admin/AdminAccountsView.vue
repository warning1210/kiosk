<template>
  <main class="page">
    <AdminSidebar active="accounts" />

    <section class="content">
      <header>
        <div>
          <p>본점 관리</p>
          <h1>지점 계정 관리</h1>
          <span>새 지점 이메일로 가입 URL을 발급하고, 계정 상태를 관리하세요.</span>
        </div>
        <button class="refresh" type="button" @click="load">새로고침</button>
      </header>

      <section class="invite-card">
        <h2>새 지점 초대</h2>
        <form class="invite-form" @submit.prevent="issueInvite">
          <input v-model.trim="newEmail" type="email" required placeholder="신규 지점장 이메일">
          <button :disabled="issuing" type="submit">{{ issuing ? '발급 중' : '초대 URL 발급' }}</button>
        </form>
      </section>

      <div class="summary">
        <article><span>발급된 초대</span><b>{{ applications.length }}</b></article>
        <article><span>가입 완료</span><b>{{ joinedCount }}</b></article>
      </div>

      <div v-if="error" class="alert">{{ error }}</div>

      <section class="list-card">
        <div class="list-head">
          <h2>지점 계정 목록</h2>
          <span>{{ loading ? '불러오는 중' : `${applications.length}건` }}</span>
        </div>

        <div v-if="!loading && !applications.length" class="empty">발급된 초대가 없습니다.</div>
        <article v-for="application in applications" :key="application.applicationId" class="application">
          <div class="identity">
            <span class="avatar">{{ (application.branchName || application.email).slice(0, 1).toUpperCase() }}</span>
            <div><strong>{{ application.branchName || '가입 전' }}</strong><p>{{ application.managerName || application.email }}</p></div>
          </div>
          <dl>
            <div><dt>이메일</dt><dd>{{ application.email }}</dd></div>
            <div><dt>주소</dt><dd>{{ application.address || '-' }}</dd></div>
            <div><dt>연락처</dt><dd>{{ application.phone || '-' }}</dd></div>
            <div><dt>사업자번호</dt><dd>{{ application.businessNumber || '-' }}</dd></div>
            <div><dt>로그인 아이디</dt><dd>{{ application.loginId || '아직 가입 전' }}</dd></div>
          </dl>
          <div class="request-state">
            <span :class="['status', stateOf(application)]">{{ stateLabel(application) }}</span>
            <small>{{ formatDate(application.appliedAt) }}</small>
          </div>
          <div class="actions">
            <button v-if="application.accountStatus === 'ACTIVE'" class="delete" :disabled="deletingId === application.applicationId" type="button" @click="deleteAccount(application)">
              {{ deletingId === application.applicationId ? '삭제 중' : '계정 삭제' }}
            </button>
            <span v-else-if="application.accountStatus === 'DELETED'" class="deleted">삭제된 계정</span>
            <button v-else class="copy" :disabled="regeneratingId === application.applicationId" type="button" @click="regenerateInvite(application)">
              {{ copiedId === application.applicationId ? '복사됨!' : regeneratingId === application.applicationId ? '생성 중' : '초대 URL 생성' }}
            </button>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'

const applications = ref([])
const loading = ref(true)
const error = ref('')
const newEmail = ref('')
const issuing = ref(false)
const deletingId = ref(null)
const regeneratingId = ref(null)
const copiedId = ref(null)
const joinedCount = computed(() => applications.value.filter(item => item.accountStatus === 'ACTIVE').length)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    applications.value = (await http.get('/hq/branch-applications')).data
  } catch {
    error.value = '지점 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function issueInvite() {
  issuing.value = true
  error.value = ''
  try {
    const { data } = await http.post('/hq/branch-applications', { email: newEmail.value })
    applications.value.unshift(data)
    newEmail.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || '초대 URL을 발급하지 못했습니다.'
  } finally {
    issuing.value = false
  }
}

async function deleteAccount(application) {
  if (!window.confirm(`${application.branchName || application.email}의 로그인 계정을 삭제할까요?\n주문·매출·재고 이력은 보존됩니다.`)) return
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

async function regenerateInvite(application) {
  regeneratingId.value = application.applicationId
  error.value = ''
  try {
    const { data } = await http.post(`/hq/branch-applications/${application.applicationId}/invite`)
    Object.assign(application, data)
    await navigator.clipboard.writeText(application.inviteUrl)
    copiedId.value = application.applicationId
    setTimeout(() => { if (copiedId.value === application.applicationId) copiedId.value = null }, 2000)
  } catch (e) {
    error.value = e.response?.data?.message || '초대 URL을 만들지 못했습니다.'
  } finally {
    regeneratingId.value = null
  }
}

function stateOf(application) {
  if (application.accountStatus === 'ACTIVE') return 'approved'
  if (application.accountStatus === 'DELETED') return 'rejected'
  return 'pending'
}

function stateLabel(application) {
  if (application.accountStatus === 'ACTIVE') return '가입 완료'
  if (application.accountStatus === 'DELETED') return '삭제된 계정'
  return '가입 대기'
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}header{display:flex;align-items:flex-end;justify-content:space-between}header p{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}h1{margin:0;font-size:27px}header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}.refresh{padding:10px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:9px;font-weight:800;cursor:pointer}
.invite-card{margin-top:24px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.invite-card h2{margin:0 0 12px;font-size:14px}.invite-form{display:flex;gap:8px}.invite-form input{flex:1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}.invite-form button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer;white-space:nowrap}.invite-form button:disabled{opacity:.55}
.summary{display:grid;grid-template-columns:repeat(2,minmax(180px,250px));gap:14px;margin:20px 0}.summary article{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.summary span{color:#798392;font-size:11px}.summary b{font-size:25px}.alert{margin-bottom:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.list-head{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #e9edf2}.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}.application{display:grid;grid-template-columns:190px minmax(330px,1fr) 105px 120px;gap:20px;align-items:center;padding:19px 22px;border-bottom:1px solid #eef1f5}.application:last-child{border:0}.identity{display:flex;align-items:center;gap:11px}.avatar{display:grid;width:40px;height:40px;flex:0 0 auto;place-items:center;color:#fff;background:linear-gradient(145deg,#ee4b9d,#6568ee);border-radius:12px;font-weight:900}.identity strong{font-size:12px}.identity p{margin:4px 0 0;color:#88919e;font-size:10px}dl{display:grid;grid-template-columns:1.4fr .8fr;gap:8px 18px;margin:0}dl div{min-width:0}dt{color:#959daa;font-size:8px}dd{overflow:hidden;margin:3px 0 0;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.request-state{display:grid;gap:7px}.status{width:max-content;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}.status.rejected{color:#c63750;background:#ffe8ed}.request-state small{color:#9aa2ad;font-size:8px}.actions button{width:100%;padding:10px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-size:10px;font-weight:900;cursor:pointer}.actions button:disabled{opacity:.55}.actions .copy{color:#5d62e8;border:1px solid #cfd3fa;background:#f5f6ff}.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.application{grid-template-columns:1fr}.summary{grid-template-columns:1fr 1fr}dl{grid-template-columns:1fr 1fr}.invite-form{flex-direction:column}}
.actions .delete{color:#d13852;border:1px solid #ffc8d1;background:#fff4f6}.deleted{color:#9aa2ad;font-size:9px;font-weight:800}
</style>
