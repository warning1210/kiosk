<template>
  <aside class="sidebar">
    <div class="brand">
      <span>♙</span>
      <div><strong>배스킨라빈스 {{ session.branchName }}</strong><small>매장 관리 시스템</small></div>
      <button
        type="button"
        class="staff-call-bell"
        :class="{ ringing: staffCalled }"
        :aria-label="staffCalled ? '직원 호출 확인' : '직원 호출 없음'"
        @click="acknowledgeStaffCall"
      >
        🔔
        <span v-if="staffCalled" class="badge"></span>
      </button>
    </div>
    <Transition name="staff-call-bubble-pop">
      <div v-if="staffCalled" class="staff-call-bubble" @click="acknowledgeStaffCall">
        🔔 고객이 직원을 호출했어요!
        <span class="bubble-hint">눌러서 확인</span>
      </div>
    </Transition>
    <p class="menu-label">메뉴</p>
    <nav>
      <RouterLink :class="{ active: active === 'dashboard' }" to="/branch/dashboard"><i>▦</i>대시보드</RouterLink>
      <RouterLink :class="{ active: active === 'orders' }" to="/branch/orders"><i>▤</i>주문 관리<b v-if="newOrders">{{ newOrders }}</b></RouterLink>
      <RouterLink :class="{ active: active === 'orderlist' }" to="/branch/orderlist"><i>▤</i>주문 내역</RouterLink>
      <RouterLink :class="{ active: active === 'inventory' }" to="/branch/inventory"><i>▣</i>재고 현황</RouterLink>
      <RouterLink :class="{ active: active === 'stock-requests' }" to="/branch/stock-requests"><i>♧</i>입고 신청 현황</RouterLink><RouterLink :class="{ active: active === 'events' }" to="/branch/events"><i>□</i>이벤트 관리</RouterLink><RouterLink :class="{ active: active === 'sales' }" to="/branch/sales"><i>⌁</i>판매 통계</RouterLink><RouterLink :class="{ active: active === 'chat' }" to="/branch/chat"><i>✉</i>본점 채팅</RouterLink>
    </nav>
    <nav class="settings"><a href="#"><i>⚙</i>매장 설정</a></nav>
    <button class="logout-btn" type="button" :disabled="loggingOut" @click="logout">
      <i>↪</i>{{ loggingOut ? '로그아웃 중...' : '로그아웃' }}
    </button>
    <div class="profile"><span>{{ managerInitial }}</span><div><strong>{{ session.managerName }}</strong><small>{{ session.branchName }} · 점장</small></div></div>
  </aside>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { signOut } from 'firebase/auth'
import { useRouter } from 'vue-router'
import { firebaseAuth } from '../../firebase'
import http from '../../api/branch'
import { useStaffCallAlert } from '../../composables/useStaffCallAlert'
defineProps({ active: String, newOrders: { type: Number, default: 0 } })
const { called: staffCalled, acknowledge: acknowledgeStaffCall } = useStaffCallAlert()
const router=useRouter()
const loggingOut=ref(false)
let presenceTimer
const session=JSON.parse(localStorage.getItem('branch-session')||'{}')
if(!session.branchName)session.branchName='지점'
if(!session.managerName)session.managerName='지점장'
const managerInitial=computed(()=>session.managerName.slice(0,1))
onMounted(()=>{
  sendPresence()
  // 화면을 조작하지 않고 열어 둔 경우에도 실제 온라인 상태가 유지되도록 신호를 보낸다.
  presenceTimer=setInterval(sendPresence,5000)
})
onBeforeUnmount(()=>clearInterval(presenceTimer))
async function sendPresence(){
  try{await http.post('/presence/heartbeat')}catch(error){
    // 상태 신호 실패만으로 지점 관리 화면의 사용을 막지 않는다.
    console.error(error)
  }
}
async function logout(){
  if(loggingOut.value)return
  loggingOut.value=true
  try{await signOut(firebaseAuth)}catch(error){console.error('Firebase 로그아웃 실패:',error)}finally{
    localStorage.removeItem('branch-session')
    await router.replace('/branch/login')
    loggingOut.value=false
  }
}
</script>
<style scoped>
.sidebar{position:fixed;inset:0 auto 0 0;z-index:10;display:flex;flex-direction:column;width:238px;border-right:1px solid #dfe4ed;background:#fff}.brand{display:flex;align-items:center;gap:10px;height:82px;padding:0 20px;color:#fff;background:linear-gradient(110deg,#ef3f91,#5d5df3)}.brand>span{display:grid;width:36px;height:36px;place-items:center;background:rgb(255 255 255/18%);border-radius:11px}.brand strong,.brand small{display:block}.brand strong{font-size:13px}.brand small{margin-top:3px;font-size:10px;opacity:.82}.staff-call-bell{position:relative;margin-left:auto;width:36px;height:36px;display:grid;place-items:center;border:none;background:rgb(255 255 255/18%);border-radius:10px;color:#fff;font-size:15px;cursor:pointer;flex-shrink:0}.staff-call-bell .badge{position:absolute;top:3px;right:4px;width:13px;height:13px;border-radius:50%;background:#ff3b30;border:2px solid #fff;animation:staff-call-badge-pulse 1.4s ease-in-out infinite}@keyframes staff-call-badge-pulse{0%{box-shadow:0 0 0 0 rgb(255 59 48/70%)}70%{box-shadow:0 0 0 9px rgb(255 59 48/0%)}100%{box-shadow:0 0 0 0 rgb(255 59 48/0%)}}.staff-call-bell.ringing{animation:staff-call-ring .6s ease-in-out infinite}@keyframes staff-call-ring{0%,100%{transform:rotate(0)}20%{transform:rotate(-15deg)}40%{transform:rotate(12deg)}60%{transform:rotate(-8deg)}80%{transform:rotate(4deg)}}.staff-call-bubble{position:absolute;top:90px;right:14px;z-index:20;width:190px;padding:12px 14px;color:#fff;background:#ff3b30;border-radius:12px;font-size:12px;font-weight:800;line-height:1.5;box-shadow:0 10px 24px rgb(255 59 48/40%);cursor:pointer;animation:staff-call-bubble-bounce 1.6s ease-in-out infinite}.staff-call-bubble::before{content:'';position:absolute;top:-7px;right:22px;width:0;height:0;border-left:7px solid transparent;border-right:7px solid transparent;border-bottom:7px solid #ff3b30}.staff-call-bubble .bubble-hint{display:block;margin-top:4px;font-size:10px;font-weight:700;opacity:.85;text-decoration:underline}@keyframes staff-call-bubble-bounce{0%,100%{transform:translateY(0)}50%{transform:translateY(-4px)}}.staff-call-bubble-pop-enter-active{animation:staff-call-bubble-pop .25s ease-out}.staff-call-bubble-pop-leave-active{transition:opacity .15s ease}.staff-call-bubble-pop-leave-to{opacity:0}@keyframes staff-call-bubble-pop{0%{transform:scale(.85);opacity:0}100%{transform:scale(1);opacity:1}}.menu-label{margin:17px 20px 7px;color:#9ba3b0;font-size:10px}nav{display:grid;gap:3px;padding:0 10px}nav a{position:relative;display:flex;align-items:center;gap:12px;padding:12px;color:#4e5868;border-radius:9px;font-size:12px;font-weight:700;text-decoration:none}nav a i{width:18px;color:#697487;font-style:normal;text-align:center}nav a.active{color:#5f63ee;background:#eef0ff}nav a.active i{color:#5f63ee}nav a b{margin-left:auto;padding:3px 7px;color:#fa4854;background:#ffe5e8;border-radius:10px;font-size:9px}.settings{margin-top:34px;border-top:1px solid #edf0f4;padding-top:15px}.logout-btn{display:flex;align-items:center;justify-content:center;gap:8px;margin:auto 10px 10px;padding:11px;border:1px solid #e2e5ea;border-radius:9px;color:#687386;background:#fff;font-size:11px;font-weight:700;cursor:pointer}.logout-btn i{font-style:normal}.logout-btn:hover{color:#e33d64;border-color:#f2b6c4;background:#fff5f7}.logout-btn:disabled{opacity:.55;cursor:wait}.profile{display:flex;align-items:center;gap:10px;padding:16px;border-top:1px solid #e9edf2}.profile>span{display:grid;width:34px;height:34px;place-items:center;color:#fff;background:#6266f2;border-radius:50%;font-weight:800}.profile strong,.profile small{display:block}.profile strong{font-size:11px}.profile small{margin-top:3px;color:#9299a5;font-size:9px}@media(max-width:760px){.sidebar{display:none}}
</style>
