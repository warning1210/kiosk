<template>
  <main class="auth">
    <aside>
      <div class="brand">♙　배스킨라빈스</div>
      <div>
        <h1>매장 운영을<br>한 곳에서.</h1>
        <p>지점 개설을 신청하면 본점 검토 후 계정 설정용 초대 URL을 안내해 드립니다.</p>
      </div>
    </aside>

    <section>
      <form @submit.prevent="submit">
        <div v-if="done" class="success">
          <b>✓</b>
          <h2>지점 개설 신청이 접수되었습니다</h2>
          <p>본점에서 신청을 수락하면 입력한 아이디와 비밀번호로 로그인할 수 있습니다.</p>
          <RouterLink to="/branch/login">로그인으로 돌아가기</RouterLink>
        </div>

        <template v-else>
          <h2>지점 개설 신청</h2>
          <p>본점 검토에 필요한 정보를 입력하세요</p>
          <div class="two">
            <label>이름<input v-model="form.managerName" required></label>
            <label>지점명<input v-model="form.branchName" required placeholder="예: 강남점"></label>
            <label>전화번호<input v-model="form.phone" required></label>
            <label>이메일<input v-model="form.email" required type="email"></label>
          </div>

          <label>매장 주소</label>
          <div class="address-row">
            <input v-model="postcode" readonly placeholder="우편번호">
            <button class="address-search" type="button" @click="searchAddress">주소 검색</button>
          </div>
          <input v-model="baseAddress" class="address-input" readonly required placeholder="주소 검색을 눌러 매장 주소를 선택하세요">
          <input ref="detailInput" v-model="detailAddress" class="address-input" placeholder="상세 주소를 입력하세요">

          <label>사업자등록번호<input v-model="form.businessNumber" required></label>

          <div class="account-title">
            <strong>로그인 계정</strong>
            <span>본점 수락 후 사용할 지점 계정을 입력하세요.</span>
          </div>
          <label>아이디<input v-model.trim="form.loginId" required minlength="4" autocomplete="username" placeholder="영문·숫자 4자 이상"></label>
          <div class="two">
            <label>비밀번호<input v-model="form.password" required minlength="8" type="password" autocomplete="new-password" placeholder="8자 이상"></label>
            <label>비밀번호 확인<input v-model="passwordConfirm" required minlength="8" type="password" autocomplete="new-password" placeholder="비밀번호 다시 입력"></label>
          </div>
          <button type="submit">본점에 개설 신청 보내기</button>
          <p v-if="error" class="error">{{ error }}</p>
          <div class="back"><RouterLink to="/branch/login">이미 계정이 있나요? 로그인</RouterLink></div>
        </template>
      </form>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/http'

const KAKAO_POSTCODE_SCRIPT = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
const done = ref(false)
const error = ref('')
const postcode = ref('')
const baseAddress = ref('')
const detailAddress = ref('')
const detailInput = ref(null)
const passwordConfirm = ref('')
const form = reactive({ managerName: '', branchName: '', phone: '', email: '', businessNumber: '', loginId: '', password: '' })

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

onMounted(() => loadPostcodeScript().catch(() => {}))

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

async function submit() {
  error.value = ''
  if (!baseAddress.value) {
    error.value = '주소 검색으로 매장 주소를 선택하세요.'
    return
  }
  if (form.password !== passwordConfirm.value) {
    error.value = '비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    return
  }
  try {
    await http.post('/branch-auth/applications', {
      ...form,
      address: [baseAddress.value, detailAddress.value].filter(Boolean).join(' ')
    })
    done.value = true
  } catch (e) {
    error.value = e.response?.data?.message || '신청을 접수하지 못했습니다.'
  }
}
</script>

<style scoped>
.auth{display:grid;min-height:100vh;grid-template-columns:34% 66%;color:#222b38}.auth aside{display:flex;padding:38px 44px;flex-direction:column;color:#fff;background:linear-gradient(155deg,#6165ee,#9959dc 50%,#ea3c9b)}.brand{font-weight:900}.auth aside>div:nth-child(2){margin:auto 0}.auth aside h1{font-size:32px}.auth aside p{font-size:11px;line-height:1.8;opacity:.8}.auth section{display:grid;padding:35px;place-items:center}form{width:min(620px,100%);padding:15px 0}h2{margin:0}form>p{color:#87919e;font-size:10px}.two{display:grid;grid-template-columns:1fr 1fr;gap:12px}label{display:grid;gap:6px;margin-top:14px;font-size:9px;font-weight:800}input{box-sizing:border-box;width:100%;padding:12px;border:1px solid #dfe3e9;border-radius:7px}.address-row{display:grid;grid-template-columns:1fr 120px;gap:8px;margin-top:6px}.address-input{margin-top:8px}.address-search{margin:0;color:#6266ef;border:1px solid #6266ef;background:#fff}.account-title{display:grid;gap:4px;margin-top:25px;padding-top:20px;border-top:1px solid #eceff4}.account-title strong{font-size:13px}.account-title span{color:#87919e;font-size:9px}button{width:100%;margin-top:22px;padding:13px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;cursor:pointer}.back{text-align:center;margin-top:18px}.back a,.success a{color:#6266ef;font-size:10px;text-decoration:none}.error{color:#c62e48}.success{text-align:center}.success>b{display:grid;width:55px;height:55px;margin:auto;place-items:center;color:#fff;background:#22ad67;border-radius:50%;font-size:28px}.success p{margin:15px 0 25px;color:#7f8996}@media(max-width:700px){.auth{grid-template-columns:1fr}.auth aside{display:none}.two{grid-template-columns:1fr}}
</style>
