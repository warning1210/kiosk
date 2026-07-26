<template>
  <aside class="sidebar">
    <div class="brand"><span>♙</span><div><strong>배스킨라빈스 본점</strong><small>본사 관리 시스템</small></div></div>
    <p class="menu-label">메뉴</p>
    <nav>
      <RouterLink :class="{ active: active === 'dashboard' }" to="/admin/dashboard"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/></svg></i>대시보드</RouterLink>
      <RouterLink :class="{ active: active === 'stock-requests' }" to="/admin/stock-requests"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><path d="M8 11h8M8 15h5"/></svg></i>재고 신청</RouterLink>
      <RouterLink :class="{ active: active === 'deliveries' }" to="/admin/deliveries"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h1"/><path d="M15 18H9"/><path d="M19 18h1a1 1 0 0 0 1-1v-3.3a1 1 0 0 0-.2-.6l-2.5-3.1a1 1 0 0 0-.8-.4H14"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/></svg></i>배송 관리</RouterLink>
      <RouterLink :class="{ active: active === 'branches' }" to="/admin/branches"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/><path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/><path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/><path d="M10 6h4M10 10h4M10 14h4M10 18h4"/></svg></i>지점 관리</RouterLink>
      <RouterLink :class="{ active: active === 'reports' }" to="/admin/reports"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="M7 15v3M12 9v9M17 5v13"/></svg></i>통계/리포트</RouterLink>
      <RouterLink :class="{ active: active === 'events' }" to="/admin/events"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 11 18-5v12L3 14v-3z"/><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6"/></svg></i>이벤트 관리</RouterLink>
      <RouterLink :class="{ active: active === 'products' }" to="/admin/products"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="M3.3 7 12 12l8.7-5"/><path d="M12 22V12"/></svg></i>상품 관리</RouterLink>
      <RouterLink :class="{ active: active === 'coupons' }" to="/admin/coupons"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/><path d="M13 5v2M13 11v2M13 17v2"/></svg></i>쿠폰 발송</RouterLink>
      <RouterLink :class="{ active: active === 'chat' }" to="/admin/chat"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg></i>채팅 상담<b v-if="unreadCount">{{ unreadCount }}</b></RouterLink>
      <RouterLink :class="{ active: active === 'notices' }" to="/admin/notices"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v5h5"/><path d="M8 13h8M8 17h8M8 9h1"/></svg></i>공지사항</RouterLink>
      <RouterLink :class="{ active: active === 'accounts' }" to="/admin/accounts"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg></i>계정 관리</RouterLink>
      <RouterLink :class="{ active: active === 'settings' }" to="/admin/settings"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg></i>시스템 설정</RouterLink>
    </nav>
    <button class="logout-btn" type="button" @click="logout"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg></i>로그아웃</button>
    <div class="profile"><span>{{ nameInitial }}</span><div><strong>{{ session.name }}</strong><small>본점 관리자</small></div></div>
  </aside>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/hq'
defineProps({ active: String })
const router = useRouter()
const session = JSON.parse(localStorage.getItem('hq-session') || '{}')
if (!session.name) session.name = '본점 관리자'
const nameInitial = computed(() => session.name.slice(0, 1))
// 모든 상담방의 미확인 지점 메시지를 합쳐 메뉴 옆 숫자 배지로 보여준다.
const unreadCount = ref(0)
let unreadTimer
onMounted(() => {
  loadUnreadCount()
  // 새 메시지를 새로고침 없이 확인할 수 있도록 3초마다 숫자를 갱신한다.
  unreadTimer = setInterval(loadUnreadCount, 3000)
})
onBeforeUnmount(() => clearInterval(unreadTimer))
async function loadUnreadCount() {
  try {
    const { data } = await http.get('/hq/chat/rooms')
    unreadCount.value = data.reduce((total, room) => total + Number(room.unreadCount || 0), 0)
  } catch (error) {
    // 배지 조회 실패만으로 다른 관리 화면의 사용을 막지 않는다.
    console.error(error)
  }
}
function logout() {
  localStorage.removeItem('hq-session')
  router.replace('/branch/login')
}
</script>
<style scoped>
.sidebar{position:fixed;inset:0 auto 0 0;z-index:10;display:flex;flex-direction:column;width:238px;overflow-y:auto;border-right:1px solid #dfe4ed;background:#fff}.brand{position:sticky;top:0;display:flex;align-items:center;gap:10px;height:82px;padding:0 20px;color:#fff;background:linear-gradient(110deg,#ef3f91,#5d5df3)}.brand>span{display:grid;width:36px;height:36px;place-items:center;background:rgb(255 255 255/18%);border-radius:11px}.brand strong,.brand small{display:block}.brand strong{font-size:13px}.brand small{margin-top:3px;font-size:10px;opacity:.82}.menu-label{margin:17px 20px 7px;color:#9ba3b0;font-size:10px}nav{display:grid;gap:3px;padding:0 10px}nav a{position:relative;display:flex;align-items:center;gap:12px;padding:12px;color:#4e5868;border-radius:9px;font-size:12px;font-weight:700;text-decoration:none}nav a i{display:grid;place-items:center;width:18px;height:18px;color:#697487;font-style:normal}nav a i svg{width:18px;height:18px;display:block}.logout-btn i svg{width:17px;height:17px;display:block}nav a.active{color:#5f63ee;background:#eef0ff}nav a.active i{color:#5f63ee}.logout-btn{display:flex;align-items:center;justify-content:center;gap:8px;margin:14px 10px 10px;padding:11px;border:1px solid #e2e5ea;border-radius:9px;color:#687386;background:#fff;font-size:11px;font-weight:700;cursor:pointer}.logout-btn i{font-style:normal}.logout-btn:hover{color:#e33d64;border-color:#f2b6c4;background:#fff5f7}.profile{position:sticky;bottom:0;display:flex;align-items:center;gap:10px;padding:16px;border-top:1px solid #e9edf2;background:#fff}.profile>span{display:grid;width:34px;height:34px;place-items:center;color:#fff;background:#6266f2;border-radius:50%;font-weight:800}.profile strong,.profile small{display:block}.profile strong{font-size:11px}.profile small{margin-top:3px;color:#9299a5;font-size:9px}@media(max-width:760px){.sidebar{display:none}}
nav a b{margin-left:auto;min-width:18px;padding:3px 6px;color:#fff;background:#fa4854;border-radius:10px;font-size:9px;text-align:center}
</style>
