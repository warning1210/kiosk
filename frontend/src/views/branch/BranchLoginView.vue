<template>
  <main class="auth">
    <aside>
      <div class="brand">
        ♙　배스킨라빈스
      </div>
      
      <div>
        <h1>다시 오신 것을<br>환영합니다.</h1>
        <p>오늘 하루도 성공적으로 함께 시작하세요.</p>
      </div>
    
    </aside>
    <section>
      <form @submit.prevent="login">
        <p style="font-size:16px;font-weight:800;color:#c52f47;background:#ffecef;padding:12px;border-radius:8px;margin-bottom:16px;">
          테스트 id/pw는 admin/admin1234 입니다!!
        </p>
        
        <h2>로그인</h2>
        <p>계정 정보를 입력하세요</p>
        <label>아이디<input v-model="loginId" required placeholder="아이디 입력"></label>
        <label>비밀번호<input v-model="password" required type="password" placeholder="비밀번호 입력"></label>
        
        <div class="options">
          <label><input v-model="remember" type="checkbox"> 로그인 상태 유지</label>
          <a href="#">ID / PW 찾기</a>
        </div>
        
        <button>로그인</button>
        <p v-if="error" class="error">
          {{ error }}
        </p>
      </form>
    </section>
  </main>
</template>

<script setup>
  import{ref}from'vue';
  import{useRouter}from'vue-router';
  import{setPersistence,browserLocalPersistence,browserSessionPersistence,signInWithEmailAndPassword}from'firebase/auth';
  import{firebaseAuth}from'../../firebase';
  import http from'../../api/http';
  
  const router=useRouter(),loginId=ref(''),password=ref(''),remember=ref(true) //,error=ref('');
  async function login(){error.value='';
                         // 보통함수는 sum(1,2); 이지만 여기는 dienfity=(명령; 명령; 명령; 명령;)으로 실행되는 것 
                         try{ // try 안에 있는 게 명령 하나하나가 통과가 되야지만
                           const identity=( // 익명함수 - 갱신이 되는 방식
                             await http.get(`/branch-auth/login-identity/${encodeURIComponent(loginId.value)}`)).data;
                           // setPersistence : 인증값을 어디에 넣어줄지 저장해주는
                           // 보안 수정 사항에서 변경 / 시간을 주고 처리하는 방식 / firebase가 막혔으면 안되니까 잘 됐다고는 못함. 보안상으로는 기본적인 부분이 좋을지도 / firebase가 털리면 다 털림
                           await setPersistence(firebaseAuth,remember.value?browserLocalPersistence:browserSessionPersistence); // 세션(날라감) 저장하느냐? 로컬(저장됨)에 저장하느냐?
                           const credential=await signInWithEmailAndPassword(firebaseAuth,identity.email,password.value); // 토큰 발급 -> 인증
                           const{data}=await http.post('/branch-auth/firebase-session',{
                             idToken:await credential.user.getIdToken(true)
                           });
                           
                           localStorage.setItem('branch-session',JSON.stringify(data)); 
                           router.replace('/branch/dashboard')
                         }
                           // try에서 하나라도 오류나면 잡힘
                         catch(e){console.error(e);
                                  error.value=e.response?.data?.message||firebaseMessage(e.code)||'로그인할 수 없습니다.'}
                        }
  
  function firebaseMessage(code){
    return({
      'auth/invalid-credential':'아이디 또는 비밀번호가 올바르지 않습니다.',
      'auth/too-many-requests':'로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요.',
      'auth/network-request-failed':'네트워크 연결을 확인하세요.'
    })
      [code]
  }
</script>

<style scoped>
  .auth{display:grid;min-height:100vh;grid-template-columns:38% 62%;color:#222b38;background:#fff}
  
  .auth aside{display:flex;padding:38px 44px;flex-direction:column;justify-content:space-between;color:#fff;background:linear-gradient(155deg,#6165ee,#9959dc 50%,#ea3c9b)}
  
  .brand{font-weight:900}
  
  .auth aside h1{font-size:34px;line-height:1.3}
  
  .auth aside p{max-width:330px;font-size:12px;line-height:1.8;opacity:.78}
  
  .auth aside small{font-size:9px;opacity:.7}
  
  .auth section{display:grid;place-items:center}
  
  form{width:min(440px,80%)}
  
  h2{margin:0;font-size:25px}
  
  form>p{margin:8px 0 28px;color:#8a94a1;font-size:11px}
  
  label{display:grid;gap:7px;margin-top:15px;font-size:10px;font-weight:800}
  
  input{padding:13px;border:1px solid #dfe3e9;border-radius:8px;outline:none}
  
  input:focus{border-color:#6266ef}
  
  .options{display:flex;align-items:center;justify-content:space-between;margin:13px 0 20px}
  
  .options label{display:flex;align-items:center;gap:6px;margin:0;color:#788391}
  
  .options a,.apply a{color:#5f64ee;text-decoration:none}
  
  form>button{width:100%;padding:13px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;box-shadow:0 8px 18px rgb(98 102 239/22%)}
  
  .error{padding:10px;color:#c52f47;background:#ffecef;border-radius:7px}
  
  .apply{text-align:center;margin-top:20px;color:#7f8995;font-size:10px}
  
  @media(max-width:700px){
    .auth{grid-template-columns:1fr}
    .auth aside{display:none}
  }
</style>
