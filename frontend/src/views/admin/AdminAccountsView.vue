<template>
  <main class="page">
    <AdminSidebar active="accounts" />

    <section class="content">
      <header>
        <div>
          <p>본점 관리</p>
          <h1>지점 계정 관리</h1>
          <span>현재 연결된 지점장 계정을 조회하고 신규 지점 관리자에게 이메일 초대를 보냅니다.</span>
        </div>
        <button class="refresh" type="button" @click="load">새로고침</button>
      </header>

      <section class="invite-card">
        <h2>새 지점 초대 메일</h2>
        <!-- 본점 관리자가 수신 이메일만 입력하면 서버가 초대 링크를 포함한 메일을 발송한다. -->
        <form class="invite-form" @submit.prevent="issueInvite">
          <input v-model.trim="newEmail" type="email" required placeholder="신규 지점장 이메일">
          <button :disabled="issuing" type="submit">{{ issuing ? '발송 중' : '초대 메일 보내기' }}</button>
        </form>
        <!-- 초대 URL은 관리 화면에 노출하지 않고 어느 이메일로 발송했는지만 확인시킨다. -->
        <div v-if="inviteSuccess" class="mail-success" role="status">
          <span aria-hidden="true">✓</span>
          <div><strong>초대 메일을 보냈습니다.</strong><p>{{ inviteSuccess }} 메일에서 신청 링크를 바로 확인할 수 있습니다.</p></div>
        </div>
      </section>

      <div class="summary">
        <article><span>연결된 지점 계정</span><b>{{ accounts.length }}</b></article>
        <article><span>가입 대기</span><b>{{ pendingCount }}</b></article>
      </div>

      <div v-if="error" class="alert">{{ error }}</div>

      <!-- 실제 DB에 생성된 모든 지점장 계정을 첨부 예시와 같은 표로 표시한다. -->
      <section class="account-card">
        <div class="list-head">
          <h2>지점 계정 목록</h2>
          <span>{{ loading ? '불러오는 중' : `${accounts.length}개 계정` }}</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>계정 아이디</th><th>지점명</th><th>지점장명</th><th>전화번호</th><th>이메일</th><th>마지막 로그인</th><th>상태</th><th>관리</th></tr></thead>
            <tbody>
              <tr v-for="account in accounts" :key="account.branchId">
                <td><strong>{{ account.loginId || '계정 미연결' }}</strong></td>
                <td>{{ account.branchName }}</td>
                <td>{{ account.managerName }}</td>
                <td>{{ account.phone || '-' }}</td>
                <td>{{ account.email || '-' }}</td>
                <td>{{ formatDate(account.lastLoginAt || account.createdAt) }}</td>
                <td><span :class="['status', account.online ? 'approved' : 'rejected']">{{ account.online ? '온라인' : '오프라인' }}</span></td>
                <td><button class="state-button" type="button" @click="openAccount(account)">상세보기</button></td>
              </tr>
              <tr v-if="!loading && !accounts.length"><td class="empty" colspan="8">현재 생성된 지점장 계정이 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 목록의 관리 버튼을 누르면 계정과 지점 DB 정보를 한곳에서 확인하는 상세 창을 연다. -->
      <div v-if="selectedAccount" class="detail-backdrop" @click.self="selectedAccount = null">
        <section class="detail-modal" role="dialog" aria-modal="true" aria-label="지점 계정 상세정보">
          <div class="detail-head">
            <div><span>지점 계정 상세</span><h2>{{ selectedAccount.branchName }}</h2></div>
            <button type="button" aria-label="닫기" @click="selectedAccount = null">×</button>
          </div>
          <h3>지점 정보</h3>
          <dl class="detail-grid">
            <!-- DB 기본키 대신 현재 목록에서 삭제된 지점을 제외한 연속 번호를 표시한다. -->
            <div><dt>지점 ID</dt><dd>{{ valueOf(selectedAccount.displayId) }}</dd></div>
            <div><dt>지점명</dt><dd>{{ valueOf(selectedAccount.branchName) }}</dd></div>
            <div><dt>지역</dt><dd>{{ valueOf(selectedAccount.region) }}</dd></div>
            <div><dt>지점장명</dt><dd>{{ valueOf(selectedAccount.managerName) }}</dd></div>
            <div class="wide"><dt>주소</dt><dd>{{ valueOf(selectedAccount.address) }}</dd></div>
            <div><dt>지점 전화번호</dt><dd>{{ valueOf(selectedAccount.branchPhone) }}</dd></div>
            <div><dt>지점 이메일</dt><dd>{{ valueOf(selectedAccount.branchEmail) }}</dd></div>
            <div><dt>영업 상태</dt><dd>{{ valueOf(selectedAccount.operationStatus) }}</dd></div>
            <div><dt>개점일</dt><dd>{{ valueOf(selectedAccount.openingDate) }}</dd></div>
            <div><dt>혼잡 여부</dt><dd>{{ selectedAccount.busy ? '혼잡' : '보통' }}</dd></div>
            <div><dt>예상 대기시간</dt><dd>{{ selectedAccount.estimatedWaitMinutes == null ? '-' : `${selectedAccount.estimatedWaitMinutes}분` }}</dd></div>
            <div><dt>키오스크 코드</dt><dd>{{ valueOf(selectedAccount.kioskCode) }}</dd></div>
            <div><dt>매장 접속 상태</dt><dd>{{ selectedAccount.online ? '온라인' : '오프라인' }}</dd></div>
            <div><dt>키오스크 최근 접속</dt><dd>{{ formatDate(selectedAccount.kioskLastAccessAt) }}</dd></div>
            <div><dt>지점 생성일</dt><dd>{{ formatDate(selectedAccount.branchCreatedAt) }}</dd></div>
            <div><dt>지점 수정일</dt><dd>{{ formatDate(selectedAccount.branchUpdatedAt) }}</dd></div>
          </dl>
          <h3>지점장 계정 정보</h3>
          <dl class="detail-grid">
            <div><dt>관리자 ID</dt><dd>{{ valueOf(selectedAccount.adminId) }}</dd></div>
            <div><dt>로그인 ID</dt><dd>{{ valueOf(selectedAccount.loginId) }}</dd></div>
            <div><dt>이름</dt><dd>{{ valueOf(selectedAccount.managerName) }}</dd></div>
            <div><dt>권한</dt><dd>{{ valueOf(selectedAccount.role) }}</dd></div>
            <div><dt>계정 전화번호</dt><dd>{{ valueOf(selectedAccount.phone) }}</dd></div>
            <div><dt>계정 이메일</dt><dd>{{ valueOf(selectedAccount.email) }}</dd></div>
            <div><dt>계정 상태</dt><dd>{{ valueOf(selectedAccount.accountStatus) }}</dd></div>
            <div><dt>마지막 로그인</dt><dd>{{ formatDate(selectedAccount.lastLoginAt) }}</dd></div>
            <div><dt>계정 생성일</dt><dd>{{ formatDate(selectedAccount.createdAt) }}</dd></div>
            <div><dt>계정 수정일</dt><dd>{{ formatDate(selectedAccount.accountUpdatedAt) }}</dd></div>
          </dl>
          <div class="detail-actions">
            <button v-if="selectedAccount.adminId" class="close-branch-button" :disabled="closingId === selectedAccount.adminId" type="button" @click="closeAccount(selectedAccount)">{{ closingId === selectedAccount.adminId ? '폐업 처리 중' : '폐업' }}</button>
            <span v-else class="unlinked">연결된 지점장 계정이 없습니다.</span>
            <button class="close-button" type="button" @click="selectedAccount = null">닫기</button>
          </div>
        </section>
      </div>

      <section class="list-card">
        <div class="list-head">
          <h2>초대 메일 발송 내역</h2>
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
            <small v-if="application.rejectionReason" class="reject-reason">사유: {{ application.rejectionReason }}</small>
          </div>
          <div class="actions">
            <div v-if="application.status === 'PENDING' && application.managerName" class="approval-actions">
              <button class="approve" type="button" @click="review(application, true)">수락</button>
              <button class="delete" type="button" @click="review(application, false)">반려</button>
            </div>
            <button v-else-if="application.accountStatus === 'ACTIVE'" class="delete" :disabled="deletingId === application.applicationId" type="button" @click="deleteAccount(application)">
              {{ deletingId === application.applicationId ? '삭제 중' : '계정 삭제' }}
            </button>
            <span v-else-if="application.accountStatus === 'DELETED'" class="deleted">삭제된 계정</span>
            <!-- 아직 가입하지 않은 수신자에게만 새 토큰이 담긴 초대 메일을 다시 보낼 수 있다. -->
            <button v-else-if="!application.managerName" class="copy" :disabled="regeneratingId === application.applicationId" type="button" @click="regenerateInvite(application)">
              {{ resentId === application.applicationId ? '재발송 완료' : regeneratingId === application.applicationId ? '발송 중' : '초대 메일 재발송' }}
            </button>
            <span v-else class="deleted">처리 완료</span>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup>
// 계산값과 화면 시작 조회에 필요한 Vue 기능을 가져온다.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
// 본점 로그인 토큰이 자동 포함되는 HTTP 모듈을 가져온다.
import http from '../../api/hq'
// 본점 공통 사이드바를 가져온다.
import AdminSidebar from '../../components/admin/AdminSidebar.vue'

// 이메일 초대 발급 내역을 보관한다.
const applications = ref([])
// 실제 생성된 모든 지점장 계정을 보관한다.
const accounts = ref([])
// 관리 열에서 선택한 지점의 상세정보를 모달에 표시한다.
const selectedAccount = ref(null)
// 계정과 초대 목록을 불러오는 중인지 표시한다.
const loading = ref(true)
// 사용자에게 보여 줄 오류 문구를 보관한다.
const error = ref('')
// 새로 초대할 지점장 이메일을 보관한다.
const newEmail = ref('')
// 초대 발급 중 중복 제출을 막는다.
const issuing = ref(false)
// 삭제 처리 중인 초대 식별자를 보관한다.
const deletingId = ref(null)
// Firebase와 DB를 영구 삭제 중인 지점장 식별자를 보관한다.
const closingId = ref(null)
// 초대 메일 재발송 중인 식별자를 보관해 같은 버튼의 중복 클릭을 막는다.
const regeneratingId = ref(null)
// 재발송이 완료된 초대 식별자를 보관한다.
const resentId = ref(null)
// 최근 초대 메일을 받은 주소를 성공 안내에 표시한다.
const inviteSuccess = ref('')
// 계정이 아직 만들어지지 않은 초대 수를 계산한다.
const pendingCount = computed(() => applications.value.filter(item => !item.accountStatus).length)

// 페이지가 열리면 실제 계정과 초대 내역을 조회한다.
let accountTimer
onMounted(() => {
  load()
  // 지점의 5초 신호를 반영해 온라인/오프라인 표시를 주기적으로 새로 고친다.
  accountTimer = setInterval(refreshAccounts, 5000)
})
onBeforeUnmount(() => clearInterval(accountTimer))

// 상세 창에는 DB 기본키가 아닌 현재 계정 목록의 연속 번호를 함께 전달한다.
function openAccount(account) {
  selectedAccount.value = { ...account, displayId: accounts.value.indexOf(account) + 1 }
}

// 전체 화면의 로딩 표시 없이 접속 상태와 계정 목록만 조용히 갱신한다.
async function refreshAccounts() {
  try {
    accounts.value = (await http.get('/hq/branch-accounts')).data
    if (selectedAccount.value) {
      const latest = accounts.value.find(account => account.adminId === selectedAccount.value.adminId)
      if (latest) selectedAccount.value = { ...latest, displayId: accounts.value.indexOf(latest) + 1 }
    }
  } catch (requestError) {
    console.error(requestError)
  }
}

// 계정 목록과 초대 내역을 동시에 새로 불러온다.
async function load() {
  // 조회 중 상태를 켠다.
  loading.value = true
  // 이전 오류 문구를 지운다.
  error.value = ''
  // 한 API가 실패해도 정상 응답을 받은 다른 목록은 화면에 표시한다.
  try {
    // 두 요청의 성공과 실패 결과를 각각 받기 위해 allSettled를 사용한다.
    const [accountResult, applicationResult] = await Promise.allSettled([
      http.get('/hq/branch-accounts'),
      http.get('/hq/branch-applications')
    ])
    // 계정 API가 성공하면 DB 지점 및 지점장 목록을 화면에 저장한다.
    if (accountResult.status === 'fulfilled') accounts.value = accountResult.value.data
    // 계정 API가 실패하면 이전 목록을 비우고 실제 HTTP 상태를 오류 문구에 남긴다.
    else {
      accounts.value = []
      error.value = requestErrorMessage('지점 계정', accountResult.reason)
    }
    // 신청 API가 성공하면 초대 및 가입 신청 목록을 화면에 저장한다.
    if (applicationResult.status === 'fulfilled') applications.value = applicationResult.value.data
    // 신청 API만 실패한 경우에도 지점 계정 목록은 그대로 표시한다.
    else {
      applications.value = []
      const message = requestErrorMessage('초대 내역', applicationResult.reason)
      error.value = error.value ? `${error.value} / ${message}` : message
    }
  } finally {
    // 성공 여부와 관계없이 로딩 상태를 끈다.
    loading.value = false
  }
}

// 실패한 API 이름과 HTTP 상태를 함께 보여 주어 로그인 만료와 서버 오류를 구분한다.
function requestErrorMessage(label, requestError) {
  // 서버 응답이 있으면 상태 코드를 읽고, 네트워크 오류면 연결 실패로 표시한다.
  const status = requestError?.response?.status || '연결 실패'
  // 백엔드가 구체적인 오류 메시지를 보냈다면 함께 표시한다.
  const detail = requestError?.response?.data?.message
  // 사용자가 어떤 요청을 점검해야 하는지 알 수 있는 최종 문구를 반환한다.
  return `${label} 조회 실패 (${status})${detail ? `: ${detail}` : ''}`
}

// 본점 관리자가 지점장 계정을 정지하거나 다시 정상화한다.
async function toggleAccount(account) {
  // 현재 정상 계정이면 정지로, 정지 계정이면 정상으로 다음 상태를 정한다.
  const status = account.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  // 실수로 계정을 막지 않도록 변경 전에 확인한다.
  if (!window.confirm(`${account.branchName} 계정을 ${status === 'ACTIVE' ? '복구' : '정지'}할까요?`)) return
  // 이전 오류를 지운다.
  error.value = ''
  // 상태 변경 오류를 화면에 표시하기 위해 예외를 처리한다.
  try {
    // 서버에서 계정 상태를 변경하고 최신 계정값을 받는다.
    const { data } = await http.patch(`/hq/branch-accounts/${account.adminId}/status`, { status })
    // 표의 기존 객체를 최신 서버값으로 덮어쓴다.
    Object.assign(account, data)
  // 상태 변경에 실패한 경우다.
  } catch (requestError) {
    // 서버 오류 메시지가 있으면 우선 표시한다.
    error.value = requestError.response?.data?.message || '계정 상태를 변경하지 못했습니다.'
  }
}

// 폐업 확인 후 Firebase 로그인과 해당 지점의 모든 DB 데이터를 영구 삭제한다.
async function closeAccount(account) {
  if (!window.confirm(`${account.branchName}을 폐업 처리할까요?\nFirebase 계정과 주문·재고·채팅 등 지점 데이터가 모두 영구 삭제되며 복구할 수 없습니다.`)) return
  closingId.value = account.adminId
  error.value = ''
  try {
    await http.delete(`/hq/branch-accounts/${account.adminId}`)
    selectedAccount.value = null
    // 삭제 뒤 목록을 다시 받아 화면용 지점 ID를 1번부터 자동으로 당긴다.
    await load()
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '폐업 처리에 실패했습니다.'
  } finally {
    closingId.value = null
  }
}

// 제출된 지점 개설 신청을 본점에서 수락하거나 반려한다.
async function review(application, approved) {
  // 버튼을 잘못 누르는 일을 막기 위해 처리 전 확인한다.
  if (!window.confirm(`${application.branchName} 신청을 ${approved ? '수락' : '반려'}할까요?`)) return
  // 반려할 때는 예비 지점장에게 전달할 사유를 필수로 입력받는다.
  const reason = approved ? '' : window.prompt('예비 지점장에게 전달할 반려 사유를 입력하세요.')
  // 반려 사유 입력을 취소하거나 빈 값으로 두면 처리하지 않는다.
  if (!approved && (!reason || !reason.trim())) return window.alert('반려 사유를 입력해 주세요.')
  // 이전 오류 메시지를 지운다.
  error.value = ''
  // 승인 API 오류를 사용자에게 알리기 위해 예외를 처리한다.
  try {
    // 수락 여부에 맞는 API 경로를 선택한다.
    const action = approved ? 'approve' : 'reject'
    // 서버가 Firebase와 DB 계정 상태를 함께 처리하도록 요청한다.
    const { data } = await http.post(`/hq/branch-applications/${application.applicationId}/${action}`, approved ? {} : { reason: reason.trim() })
    // 수락이면 최신 결과를 반영하고, 반려이면 서버에서 삭제됐으므로 전체 목록만 갱신한다.
    if (approved) Object.assign(application, data)
    // 수락 계정 표시와 반려 데이터 제거를 위해 전체 목록을 다시 읽는다.
    await load()
  // 승인 또는 반려 처리에 실패한 경우다.
  } catch (requestError) {
    // 서버가 전달한 구체적인 오류가 있으면 우선 표시한다.
    error.value = requestError.response?.data?.message || '신청을 처리하지 못했습니다.'
  }
}

// 입력한 이메일로 초대 메일을 보내고 URL 대신 발송 결과만 화면에 표시한다.
async function issueInvite() {
  // 요청이 끝날 때까지 발송 버튼을 비활성화한다.
  issuing.value = true
  // 이전 오류와 성공 메시지가 새 요청 결과와 섞이지 않도록 초기화한다.
  error.value = ''
  inviteSuccess.value = ''
  try {
    // 입력창을 비운 뒤에도 성공 안내에 수신 주소를 표시할 수 있도록 값을 보관한다.
    const invitedEmail = newEmail.value
    // 서버가 일회용 토큰을 만들고 해당 주소로 HTML 초대 메일을 보내도록 요청한다.
    const { data } = await http.post('/hq/branch-applications', { email: newEmail.value })
    // 초대 URL은 관리 화면에 노출하지 않고 실제 수신 이메일만 확인시킨다.
    inviteSuccess.value = invitedEmail
    applications.value.unshift(data)
    newEmail.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || '초대 메일을 보내지 못했습니다.'
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
    // 삭제된 계정이 위쪽 지점 계정 목록에서도 즉시 사라지도록 두 목록을 다시 조회한다.
    selectedAccount.value = null
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || '지점 계정을 삭제하지 못했습니다.'
  } finally {
    deletingId.value = null
  }
}

// 만료되었거나 다시 보내야 하는 초대에 새 토큰을 발급해 동일 이메일로 재발송한다.
async function regenerateInvite(application) {
  // 처리 중인 행만 발송 중 상태로 바꾼다.
  regeneratingId.value = application.applicationId
  error.value = ''
  try {
    // 재발급 API는 기존 토큰을 무효화하고 새 링크를 포함한 메일을 발송한다.
    const { data } = await http.post(`/hq/branch-applications/${application.applicationId}/invite`)
    // 서버가 반환한 최신 만료 시각과 상태를 현재 목록에 반영한다.
    Object.assign(application, data)
    // 재발송 성공 문구를 잠시 표시해 본점 관리자가 결과를 바로 확인하게 한다.
    resentId.value = application.applicationId
    setTimeout(() => { if (resentId.value === application.applicationId) resentId.value = null }, 2000)
  } catch (e) {
    error.value = e.response?.data?.message || '초대 메일을 다시 보내지 못했습니다.'
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
  if (application.managerName) return '승인 대기'
  if (application.status === 'REJECTED') return '반려'
  return '가입 대기'
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

// null 또는 빈 DB 값은 상세 화면에서 알아보기 쉽게 대시로 표시한다.
function valueOf(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}header{display:flex;align-items:flex-end;justify-content:space-between}header p{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}h1{margin:0;font-size:27px}header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}.refresh{padding:10px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:9px;font-weight:800;cursor:pointer}
.invite-card{margin-top:24px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.invite-card h2{margin:0 0 12px;font-size:14px}.invite-form{display:flex;gap:8px}.invite-form input{flex:1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}.invite-form button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer;white-space:nowrap}.invite-form button:disabled{opacity:.55}
/* 초대 URL 대신 메일 발송 성공과 수신 주소를 한눈에 확인시키는 안내 영역이다. */
.mail-success{display:flex;align-items:center;gap:11px;margin-top:13px;padding:13px 14px;color:#176b45;background:#eaf8f1;border:1px solid #c8ead8;border-radius:9px}.mail-success>span{display:grid;width:28px;height:28px;flex:0 0 auto;color:#fff;background:#1ba368;border-radius:50%;place-items:center;font-size:14px;font-weight:900}.mail-success strong{display:block;font-size:11px}.mail-success p{margin:4px 0 0;color:#4c7462;font-size:10px}.reject-reason{color:#c63750!important}
.summary{display:grid;grid-template-columns:repeat(2,minmax(180px,250px));gap:14px;margin:20px 0}.summary article{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.summary span{color:#798392;font-size:11px}.summary b{font-size:25px}.alert{margin-bottom:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.list-head{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #e9edf2}.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}.application{display:grid;grid-template-columns:190px minmax(330px,1fr) 105px 120px;gap:20px;align-items:center;padding:19px 22px;border-bottom:1px solid #eef1f5}.application:last-child{border:0}.identity{display:flex;align-items:center;gap:11px}.avatar{display:grid;width:40px;height:40px;flex:0 0 auto;place-items:center;color:#fff;background:linear-gradient(145deg,#ee4b9d,#6568ee);border-radius:12px;font-weight:900}.identity strong{font-size:12px}.identity p{margin:4px 0 0;color:#88919e;font-size:10px}dl{display:grid;grid-template-columns:1.4fr .8fr;gap:8px 18px;margin:0}dl div{min-width:0}dt{color:#959daa;font-size:8px}dd{overflow:hidden;margin:3px 0 0;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.request-state{display:grid;gap:7px}.status{width:max-content;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}.status.rejected{color:#c63750;background:#ffe8ed}.request-state small{color:#9aa2ad;font-size:8px}.actions button{width:100%;padding:10px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-size:10px;font-weight:900;cursor:pointer}.actions button:disabled{opacity:.55}.actions .copy{color:#5d62e8;border:1px solid #cfd3fa;background:#f5f6ff}.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.application{grid-template-columns:1fr}.summary{grid-template-columns:1fr 1fr}dl{grid-template-columns:1fr 1fr}.invite-form{flex-direction:column}}
.actions .delete{color:#d13852;border:1px solid #ffc8d1;background:#fff4f6}.deleted{color:#9aa2ad;font-size:9px;font-weight:800}
.approval-actions{display:flex;gap:6px}.approval-actions .approve{color:#fff;background:#0b9654}.approval-actions button{width:auto;flex:1}
/* 실제 지점장 계정 표의 카드와 상태 관리 버튼 스타일이다. */
.account-card{overflow:hidden;margin-bottom:20px;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse;font-size:10px}th,td{padding:13px 14px;border-bottom:1px solid #edf0f4;text-align:left;white-space:nowrap}th{color:#7d8796;background:#fafbfc}.state-button{padding:6px 9px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.unlinked{color:#c27812;font-size:9px;font-weight:800}
/* 상세 창은 목록 위에 고정하고 바깥 영역을 눌러도 닫히게 한다. */
.detail-backdrop{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;padding:24px;background:rgb(20 27 42/48%)}
.detail-modal{width:min(850px,100%);max-height:90vh;overflow-y:auto;padding:24px;background:#fff;border-radius:18px;box-shadow:0 24px 70px rgb(20 27 42/24%)}
.detail-head{display:flex;align-items:flex-start;justify-content:space-between;padding-bottom:16px;border-bottom:1px solid #e8ecf2}.detail-head span{color:#6266ef;font-size:10px;font-weight:900}.detail-head h2{margin:5px 0 0;font-size:22px}.detail-head>button{width:34px;height:34px;color:#687181;border:1px solid #dfe4ec;background:#fff;border-radius:9px;font-size:22px;cursor:pointer}
.detail-modal h3{margin:22px 0 12px;font-size:13px}.detail-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:0;border-top:1px solid #e7ebf1;border-left:1px solid #e7ebf1}.detail-grid>div{padding:11px 13px;border-right:1px solid #e7ebf1;border-bottom:1px solid #e7ebf1;background:#fff}.detail-grid .wide{grid-column:1/-1}.detail-grid dt{font-size:9px}.detail-grid dd{font-size:11px;white-space:normal;word-break:break-all}
.detail-actions{display:flex;align-items:center;justify-content:flex-end;gap:9px;margin-top:20px}.detail-actions .state-button,.detail-actions .close-button{padding:9px 14px;border-radius:8px;font-size:10px;font-weight:800;cursor:pointer}.detail-actions .close-button{color:#fff;border:1px solid #6266ef;background:#6266ef}
.close-branch-button{padding:9px 14px;color:#d13852;border:1px solid #ffc8d1;background:#fff4f6;border-radius:8px;font-size:10px;font-weight:800;cursor:pointer}.close-branch-button:disabled{opacity:.55;cursor:wait}
@media(max-width:700px){.detail-backdrop{padding:10px}.detail-modal{padding:17px}.detail-grid{grid-template-columns:1fr}.detail-grid .wide{grid-column:auto}}
</style>
