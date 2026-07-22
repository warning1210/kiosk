<template>
<main class="auth"><aside><div class="brand">♙　배스킨라빈스</div><div><h1>다시 오신 것을<br>환영합니다.</h1><p>오늘 하루도 성공적으로 함께 시작하세요.</p></div></aside><section>
    <form @submit.prevent="login">
        <p style="font-size:16px;font-weight:800;color:#c52f47;background:#ffecef;padding:12px;border-radius:8px;margin-bottom:16px;">
            테스트 id/pw는 admin/admin1234 입니다!!<br>
            본점 id/pw는 hadmin/hadmin1234 입니다!!
        </p><h2>로그인</h2><p>계정 정보를 입력하세요</p>
<label>아이디 또는 이메일
    <input v-model="loginId" required placeholder="아이디 또는 이메일 입력">
</label>
<label>비밀번호
    <input v-model="password" required type="password" placeholder="비밀번호 입력">
</label>
<div class="options">
    <label>
        <input v-model="remember" type="checkbox"> 로그인 상태 유지
    </label>
    <a href="#">ID / PW 찾기</a>  
</div>

    <button>로그인</button>
    <p v-if="error" class="error">{{ error }}</p>
    </form>
</section>
</main>   
</template>

<script setup>
import{ref}from'vue';
import{useRoute,useRouter}from'vue-router';
import{setPersistence,browserLocalPersistence,browserSessionPersistence,signInWithEmailAndPassword}from'firebase/auth';
import{firebaseAuth}from'../../firebase';
import http from'../../api/http';

const router = useRouter()
// 로그인 전에 사용자가 열려고 했던 본점 주소를 확인하기 위해 현재 경로 정보를 받는다.
const route = useRoute()
const loginId = ref('')
const password = ref('')
const remember = ref(true)
const error = ref('')

// 본사/지점 로그인 페이지를 하나로 합친 것 - 아이디가 본사 계정인지 지점 계정인지는
// 미리 나누지 않고, 각 경로를 순서대로 시도해보고 "성공한 경로"로 role을 판단한다.
async function login() {
  error.value = ''
  const attempts = loginId.value.includes('@')
    ? [loginAsHq, loginWithFirebase]
    : [loginAsHq, loginWithDb]

  let lastError = null
  for (const attempt of attempts) {
    try {
      await attempt()
      return
    } catch (e) {
      lastError = e
    }
  }
  console.error(lastError)
  error.value = lastError?.response?.data?.message || firebaseMessage(lastError?.code) || '로그인할 수 없습니다.'
}

// 본사(HQ_ADMIN/SUPER_ADMIN) 계정이면 여기서 처리하고 끝난다 - 아이디/이메일 둘 다 지원(HqAuthService)
async function loginAsHq() {
  const { data } = await http.post('/hq-auth/login', { loginId: loginId.value, password: password.value })
  localStorage.setItem('hq-session', JSON.stringify(data))
  // 인증 때문에 로그인 화면으로 이동됐다면 로그인 후 원래 본점 화면으로 돌아간다.
  const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin/')
    ? route.query.redirect
    : '/admin/dashboard'
  // 검증된 본점 경로 또는 기본 대시보드로 이동한다.
  router.replace(redirect)
}

// 입력값 자체가 이메일이므로, 로그인 아이디로 이메일을 조회하는 과정 없이 바로 Firebase에 로그인한다
async function loginWithFirebase() {
  await setPersistence(firebaseAuth, remember.value ? browserLocalPersistence : browserSessionPersistence)
  const credential = await signInWithEmailAndPassword(firebaseAuth, loginId.value, password.value)
  const { data } = await http.post('/branch-auth/firebase-session', {
    idToken: await credential.user.getIdToken(true)
  })
  localStorage.setItem('branch-session', JSON.stringify(data))
  router.replace('/branch/dashboard')
}

// Firebase 서버가 응답하지 않는 상황을 위한 폴백 - DB에 저장된 비밀번호 해시와 직접 대조한다
async function loginWithDb() {
  const { data } = await http.post('/branch-auth/db-login', {
    loginId: loginId.value,
    password: password.value
  })
  localStorage.setItem('branch-session', JSON.stringify(data))
  router.replace('/branch/dashboard')
}

function firebaseMessage(code) {
  return ({
    'auth/invalid-credential': '아이디 또는 비밀번호가 올바르지 않습니다.',
    'auth/too-many-requests': '로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요.',
    'auth/network-request-failed': '네트워크 연결을 확인하세요.'
  })[code]
}
</script>



<style scoped>
.auth{
    display:grid;
    min-height:100vh;
    grid-template-columns:38% 62%;
    color:#222b38;background:#fff}
.auth aside{
    display:flex;padding:38px 44px;
    flex-direction:column;
    justify-content:space-between;
    color:#fff;
    background:linear-gradient(155deg,#6165ee,#9959dc 50%,#ea3c9b)
    }.brand{font-weight:900}
.auth aside h1{
    font-size:34px;line-height:1.3
    }
.auth aside p{
    max-width:330px;
    font-size:12px;
    line-height:1.8;
    opacity:.78}
.auth aside small{
    font-size:9px;opacity:.7}
.auth section{
    display:grid;
    place-items:center
    }
form{width:min(440px,80%)}h2{margin:0;font-size:25px}form>p{margin:8px 0 28px;color:#8a94a1;font-size:11px}label{display:grid;gap:7px;margin-top:15px;font-size:10px;font-weight:800}input{padding:13px;border:1px solid #dfe3e9;border-radius:8px;outline:none}input:focus{border-color:#6266ef}.options{display:flex;align-items:center;justify-content:space-between;margin:13px 0 20px}.options label{display:flex;align-items:center;gap:6px;margin:0;color:#788391}.options a,.apply a{color:#5f64ee;text-decoration:none}form>button{width:100%;padding:13px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;box-shadow:0 8px 18px rgb(98 102 239/22%)}.error{padding:10px;color:#c52f47;background:#ffecef;border-radius:7px}.apply{text-align:center;margin-top:20px;color:#7f8995;font-size:10px}@media(max-width:700px){.auth{grid-template-columns:1fr}.auth aside{display:none}}</style>
