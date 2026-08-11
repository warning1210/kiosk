<template>
  <!-- 이 키오스크 기기가 아직 어느 지점 소속인지 모르면(최초 부팅), 광고 화면 대신 지점 로그인만 보여준다 -->
  <main v-if="!kioskSession" class="setup">
    <form class="setup-form" @submit.prevent="loginKiosk">
      <h1>키오스크 지점 등록</h1>
      <p>이 키오스크가 소속된 지점의 관리자 계정으로 로그인해주세요. 최초 1회만 하면 됩니다.</p>
      <p>admin/admin1234</p>
      <label>아이디 또는 이메일<input v-model="loginId" required placeholder="아이디 또는 이메일 입력" @focus="openKeypad('id')"></label>
      <VirtualKeypad v-if="activeKeypad === 'id'" v-model="loginId" @close="activeKeypad = null" />
      <label>비밀번호<input v-model="password" required type="password" placeholder="비밀번호 입력" @focus="openKeypad('password')"></label>
      <VirtualKeypad v-if="activeKeypad === 'password'" v-model="password" @close="activeKeypad = null" />
      <div class="cf-turnstile" ref="turnstileEl"></div>
      <button :disabled="loggingIn || !turnstileToken" type="submit">{{ loggingIn ? '확인 중...' : '등록' }}</button>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </main>

  <main v-else class="home" aria-label="키오스크 광고" @click="startOrder">
    <div v-if="isBusy" class="busy-banner">
      현재 매장이 혼잡하여 주문이 지연될 수 있습니다 (약 {{ estimatedWaitMinutes }}분)
    </div>

    <Transition name="ad-fade" mode="out-in">
      <img
        :key="currentAdIndex"
        class="advertisement"
        :src="advertisements[currentAdIndex].src"
        :alt="advertisements[currentAdIndex].alt"
      />
    </Transition>

    <div class="pagination" aria-hidden="true">
      <span
        v-for="(_, index) in advertisements"
        :key="index"
        :class="{ active: index === currentAdIndex }"
      />
    </div>

    <button type="button" class="branch-switch" @click.stop="goBranchLogin">
      지점 관리자로 전환
    </button>
  </main>
</template>

<script setup>
import { onMounted, onBeforeUnmount, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { setPersistence, browserLocalPersistence, signInWithEmailAndPassword, signOut } from 'firebase/auth'
import { firebaseAuth } from '../firebase'
import http from '../api/http'
import { useCartStore } from '../stores/cart'
import { getKioskSession, setKioskSession } from '../utils/kioskSession'
import { useBranchBusyBanner } from '../composables/useBranchBusyBanner'
import VirtualKeypad from '../components/common/VirtualKeypad.vue'
import ad1 from '../assets/kiosk/ads/ad-1.png'
import ad2 from '../assets/kiosk/ads/ad-2.png'
import ad3 from '../assets/kiosk/ads/ad-3.png'
import ad4 from '../assets/kiosk/ads/ad-4.png'

const ROTATION_INTERVAL_MS = 10_000
const advertisements = [
  { src: ad1, alt: '기다림 없이 바로 주문' },
  { src: ad2, alt: '봄날의 달콤함을 좋아하세요?' },
  { src: ad3, alt: '대한민국을 응원합니다. Go Korea!' },
  { src: ad4, alt: '함께 즐기는 패밀리팩' }
]

const router = useRouter()
const cart = useCartStore()
const currentAdIndex = ref(0)
let rotationTimer

const { isBusy, estimatedWaitMinutes } = useBranchBusyBanner()

const kioskSession = ref(getKioskSession())
const loginId = ref('')
const password = ref('')
const loggingIn = ref(false)
const error = ref('')
// 아이디/비밀번호 중 어느 입력란에 가상 키패드를 띄울지 - 한 번에 하나만 연다.
const activeKeypad = ref(null)
function openKeypad(name) {
  activeKeypad.value = name
}

const turnstileToken = ref('')
const turnstileEl = ref(null)
const TURNSTILE_SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY
let widgetId = null

// BranchLoginView와 동일한 이유로 마운트마다 명시적으로 render()를 호출한다 - api.js는
// 로드 시점에 페이지를 한 번만 스캔하기 때문에 라우팅으로 재진입하면 위젯이 안 뜬다.
function renderTurnstile() {
  if (!window.turnstile || !turnstileEl.value) return
  widgetId = window.turnstile.render(turnstileEl.value, {
    sitekey: TURNSTILE_SITE_KEY,
    theme: 'light',
    callback: (token) => { turnstileToken.value = token },
    'expired-callback': () => { turnstileToken.value = '' }
  })
}

onMounted(() => {
  rotationTimer = window.setInterval(() => {
    currentAdIndex.value = (currentAdIndex.value + 1) % advertisements.length
  }, ROTATION_INTERVAL_MS)

  if (window.turnstile) {
    renderTurnstile()
    return
  }
  let script = document.querySelector('script[data-turnstile-api]')
  if (!script) {
    script = document.createElement('script')
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.dataset.turnstileApi = 'true'
    document.head.appendChild(script)
  }
  script.addEventListener('load', renderTurnstile, { once: true })
})

onBeforeUnmount(() => {
  if (widgetId) window.turnstile?.remove(widgetId)
})

onUnmounted(() => {
  window.clearInterval(rotationTimer)
})

function startOrder() {
  cart.clear()
  router.push('/kiosk/order')
}

function goBranchLogin() {
  router.push('/branch/login')
}

// @가 있으면 이메일로 보고 Firebase 로그인, 없으면 아이디로 보고 DB 폴백 로그인 (지점 로그인과 동일 규칙).
// 성공하면 branchId/branchName만 기억해두고, Firebase 세션은 즉시 로그아웃한다 -
// 손님이 만지는 키오스크 화면에 관리자 자격증명(토큰)이 남아있으면 안 되기 때문이다.
async function loginKiosk() {
  if (!turnstileToken.value) {
    error.value = '본인 인증을 먼저 완료해주세요.'
    return
  }
  loggingIn.value = true
  error.value = ''
  try {
    let data
    if (loginId.value.includes('@')) {
      await setPersistence(firebaseAuth, browserLocalPersistence)
      const credential = await signInWithEmailAndPassword(firebaseAuth, loginId.value, password.value)
      const idToken = await credential.user.getIdToken(true)
      data = (await http.post('/branch-auth/firebase-session', { idToken, turnstileToken: turnstileToken.value })).data
      await signOut(firebaseAuth)
    } else {
      data = (await http.post('/branch-auth/db-login', { loginId: loginId.value, password: password.value, turnstileToken: turnstileToken.value })).data
    }
    const session = { branchId: data.branchId, branchName: data.branchName }
    // 영업 종료로 INACTIVE였던 키오스크도 다음 등록 성공 시 다시 주문 가능한 상태로 연다.
    await http.patch('/kiosk/session/start', null, { params: { branchId: session.branchId } })
    setKioskSession(session)
    kioskSession.value = session
  } catch (e) {
    console.error(e)
    error.value = e.response?.data?.message || firebaseMessage(e.code) || '로그인할 수 없습니다.'
    // Turnstile 토큰은 1회용이라 실패한 토큰으로는 재시도해도 서버에서 다시 거부된다 - 위젯을 리셋해 새 토큰을 받게 한다.
    if (widgetId) window.turnstile?.reset(widgetId)
    turnstileToken.value = ''
  } finally {
    loggingIn.value = false
  }
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
.setup {
  display: grid;
  place-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  background: #f7f0ed;
}

.setup-form {
  width: min(420px, 90vw);
  padding: 40px 36px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 12px 32px rgb(0 0 0 / 8%);
}

.setup-form h1 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #f20c93;
}

.setup-form > p {
  margin: 0 0 24px;
  color: #888;
  font-size: 13px;
  line-height: 1.5;
}

.setup-form label {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  font-size: 12px;
  font-weight: 700;
  color: #333;
}

.setup-form input {
  padding: 13px;
  border: 1px solid #dfe3e9;
  border-radius: 8px;
  font-size: 14px;
}

.setup-form .cf-turnstile {
  margin-top: 16px;
}

.setup-form button {
  width: 100%;
  margin-top: 24px;
  padding: 14px;
  color: #fff;
  border: 0;
  background: #f20c93;
  border-radius: 8px;
  font-weight: 800;
  font-size: 14px;
  cursor: pointer;
}

.setup-form button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.setup-form .error {
  margin-top: 14px;
  padding: 10px;
  color: #c52f47;
  background: #ffecef;
  border-radius: 7px;
  font-size: 12px;
}

.home {
  position: relative;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  background: #f7f0ed;
  cursor: pointer;
}

.advertisement {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ad-fade-enter-active,
.ad-fade-leave-active {
  transition: opacity 0.6s ease;
}

.ad-fade-enter-from,
.ad-fade-leave-to {
  opacity: 0;
}

.pagination {
  position: absolute;
  bottom: 82px;
  left: 50%;
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgb(0 0 0 / 20%);
  transform: translateX(-50%);
}

.pagination span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgb(255 255 255 / 55%);
  transition: width 0.25s ease, border-radius 0.25s ease, background 0.25s ease;
}

.pagination span.active {
  width: 24px;
  border-radius: 999px;
  background: #fff;
}

.busy-banner {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 5;
  padding: 14px 20px;
  color: #fff;
  background: rgb(220 30 30 / 88%);
  backdrop-filter: blur(4px);
  font-size: 15px;
  font-weight: 700;
  text-align: center;
}

.branch-switch {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 6;
  padding: 9px 14px;
  color: rgb(255 255 255 / 85%);
  border: 1px solid rgb(255 255 255 / 35%);
  background: rgb(0 0 0 / 25%);
  border-radius: 999px;
  backdrop-filter: blur(8px);
  font-size: 12px;
  cursor: pointer;
}

@media (prefers-reduced-motion: reduce) {
  .ad-fade-enter-active,
  .ad-fade-leave-active {
    transition: none;
  }
}
</style>
