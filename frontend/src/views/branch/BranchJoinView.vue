<template>
  <main>
    <section>
      <div class="brand">♙　배스킨라빈스</div>
      <p v-if="loading">초대 정보를 확인하고 있습니다.</p>

      <form v-else-if="invite" @submit.prevent="join">
        <span class="approved">✓ 본점에서 발급한 초대 URL입니다</span>
        <h1>지점 회원가입</h1>
        <p>{{ invite.email }} 계정으로 지점 정보를 입력하고 로그인 계정을 설정하세요.</p>

        <div class="two">
          <label>이름<input v-model="managerName" required></label>
          <label>지점명<input v-model="branchName" required placeholder="예: 강남점"></label>
        </div>
        <label>전화번호<input v-model="phone" required></label>

        <label>매장 주소</label>
        <div class="address-row">
          <input v-model="postcode" readonly placeholder="우편번호">
          <button class="address-search" type="button" @click="searchAddress">주소 검색</button>
        </div>
        <input v-model="baseAddress" class="address-input" readonly required placeholder="주소 검색을 눌러 매장 주소를 선택하세요">
        <input ref="detailInput" v-model="detailAddress" class="address-input" placeholder="상세 주소를 입력하세요">

        <label>사업자등록번호<input v-model="businessNumber" required></label>

        <div class="account-title">
          <strong>로그인 계정</strong>
          <span>앞으로 로그인할 때 사용할 지점 계정을 입력하세요.</span>
        </div>
        <label>아이디<input v-model.trim="loginId" required minlength="4" autocomplete="username" placeholder="영문·숫자 4자 이상"></label>
        <div class="two">
          <label>비밀번호<input v-model="password" required minlength="8" type="password" autocomplete="new-password" placeholder="8자 이상"></label>
          <label>비밀번호 확인<input v-model="passwordConfirm" required minlength="8" type="password" autocomplete="new-password" placeholder="비밀번호 다시 입력"></label>
        </div>

        <button>가입 완료</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>

      <div v-else class="invalid">
        <h2>초대 URL을 사용할 수 없습니다</h2>
        <p>{{ error }}</p>
        <RouterLink to="/branch/login">로그인으로 이동</RouterLink>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'

const KAKAO_POSTCODE_SCRIPT = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const invite = ref(null)
const error = ref('')

const managerName = ref('')
const branchName = ref('')
const phone = ref('')
const postcode = ref('')
const baseAddress = ref('')
const detailAddress = ref('')
const detailInput = ref(null)
const businessNumber = ref('')
const loginId = ref('')
const password = ref('')
const passwordConfirm = ref('')

onMounted(async () => {
  try {
    invite.value = (await http.get(`/branch-auth/invites/${route.query.token}`)).data
  } catch (e) {
    error.value = e.response?.data?.message || '만료되었거나 잘못된 URL입니다.'
  } finally {
    loading.value = false
  }
  loadPostcodeScript().catch(() => {})
})

function loadPostcodeScript() {
  if (window.kakao?.Postcode) return Promise.resolve()
  const existing = document.querySelector(`script[src="${KAKAO_POSTCODE_SCRIPT}"]`)
  if (existing) return new Promise((resolve, reject) => {
    existing.addEventListener('load', resolve, { once: true })
    existing.addEventListener('error', reject, { once: true })
  })
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = KAKAO_POSTCODE_SCRIPT
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

async function searchAddress() {
  error.value = ''
  try {
    await loadPostcodeScript()
    new window.kakao.Postcode({
      oncomplete(data) {
        postcode.value = data.zonecode
        baseAddress.value = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress
        requestAnimationFrame(() => detailInput.value?.focus())
      }
    }).open()
  } catch {
    error.value = '주소 검색 서비스를 불러오지 못했습니다. 인터넷 연결을 확인하세요.'
  }
}

async function join() {
  error.value = ''
  if (!baseAddress.value) {
    error.value = '주소 검색으로 매장 주소를 선택하세요.'
    return
  }
  if (password.value !== passwordConfirm.value) {
    error.value = '비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    return
  }
  try {
    // 신청 정보를 서버에 제출하면 계정은 본점 승인 전까지 잠긴 상태로 만들어진다.
    await http.post('/branch-auth/join', {
      token: route.query.token,
      loginId: loginId.value,
      password: password.value,
      managerName: managerName.value,
      branchName: branchName.value,
      phone: phone.value,
      address: [baseAddress.value, detailAddress.value].filter(Boolean).join(' '),
      businessNumber: businessNumber.value
    })
    // 제출 즉시 로그인하지 않고 본점 승인 대기 안내를 보여 준다.
    window.alert('지점 개설 신청이 접수되었습니다. 본점 승인 후 로그인할 수 있습니다.')
    // 승인 뒤 다시 로그인할 수 있도록 로그인 화면으로 이동한다.
    router.replace('/branch/login')
  } catch (e) {
    error.value = e.response?.data?.message || '계정을 만들 수 없습니다.'
  }
}
</script>

<style scoped>
main{display:grid;min-height:100vh;padding:25px;place-items:center;background:#f2f5fa}
section{width:min(620px,100%);padding:34px;background:#fff;border-radius:16px;box-shadow:0 18px 50px rgb(38 51 71/12%)}
.brand{margin-bottom:30px;color:#6266ef;font-weight:900}
.approved{display:inline-block;padding:7px 10px;color:#168c50;background:#e8f8f0;border-radius:7px;font-size:9px;font-weight:800}
h1{margin:18px 0 5px}
form>p{color:#87919e;font-size:10px}
.two{display:grid;grid-template-columns:1fr 1fr;gap:12px}
label{display:grid;gap:6px;margin-top:14px;font-size:9px;font-weight:800}
input{box-sizing:border-box;width:100%;padding:12px;border:1px solid #dfe3e9;border-radius:7px}
.address-row{display:grid;grid-template-columns:1fr 120px;gap:8px;margin-top:6px}
.address-input{margin-top:8px}
.address-search{margin:0;color:#6266ef;border:1px solid #6266ef;background:#fff}
.account-title{display:grid;gap:4px;margin-top:25px;padding-top:20px;border-top:1px solid #eceff4}
.account-title strong{font-size:13px}
.account-title span{color:#87919e;font-size:9px}
button{width:100%;margin-top:22px;padding:13px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;cursor:pointer}
.error{color:#c83249}
.invalid{text-align:center}
.invalid a{color:#6266ef}
@media(max-width:700px){.two{grid-template-columns:1fr}}
</style>
