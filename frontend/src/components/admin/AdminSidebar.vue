<template>
  <aside class="sidebar">
    <div class="brand"><span>♙</span><div><strong>배스킨라빈스 본점</strong><small>본사 관리 시스템</small></div></div>
    <p class="menu-label">메뉴</p>
    <nav>
      <RouterLink :class="{ active: active === 'dashboard' }" to="/admin/dashboard"><i>▦</i>대시보드</RouterLink>
      <RouterLink :class="{ active: active === 'accounts' }" to="/admin/accounts"><i>▤</i>지점 계정</RouterLink>
      <RouterLink :class="{ active: active === 'events' }" to="/admin/events"><i>□</i>이벤트 관리</RouterLink>
      <RouterLink :class="{ active: active === 'chat' }" to="/admin/chat"><i>✉</i>지점 채팅</RouterLink>
    </nav>
    <button class="logout-btn" type="button" @click="logout"><i>↪</i>로그아웃</button>
    <div class="profile"><span>{{ nameInitial }}</span><div><strong>{{ session.name }}</strong><small>본점 관리자</small></div></div>
  </aside>
</template>
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
defineProps({ active: String })
const router = useRouter()
const session = JSON.parse(localStorage.getItem('hq-session') || '{}')
if (!session.name) session.name = '본점 관리자'
const nameInitial = computed(() => session.name.slice(0, 1))
function logout() {
  localStorage.removeItem('hq-session')
  router.replace('/admin/login')
}
</script>
<style scoped>
.sidebar{position:fixed;inset:0 auto 0 0;z-index:10;display:flex;flex-direction:column;width:238px;border-right:1px solid #dfe4ed;background:#fff}.brand{display:flex;align-items:center;gap:10px;height:82px;padding:0 20px;color:#fff;background:linear-gradient(110deg,#ef3f91,#5d5df3)}.brand>span{display:grid;width:36px;height:36px;place-items:center;background:rgb(255 255 255/18%);border-radius:11px}.brand strong,.brand small{display:block}.brand strong{font-size:13px}.brand small{margin-top:3px;font-size:10px;opacity:.82}.menu-label{margin:17px 20px 7px;color:#9ba3b0;font-size:10px}nav{display:grid;gap:3px;padding:0 10px}nav a{position:relative;display:flex;align-items:center;gap:12px;padding:12px;color:#4e5868;border-radius:9px;font-size:12px;font-weight:700;text-decoration:none}nav a i{width:18px;color:#697487;font-style:normal;text-align:center}nav a.active{color:#5f63ee;background:#eef0ff}nav a.active i{color:#5f63ee}.logout-btn{display:flex;align-items:center;justify-content:center;gap:8px;margin:auto 10px 10px;padding:11px;border:1px solid #e2e5ea;border-radius:9px;color:#687386;background:#fff;font-size:11px;font-weight:700;cursor:pointer}.logout-btn i{font-style:normal}.logout-btn:hover{color:#e33d64;border-color:#f2b6c4;background:#fff5f7}.profile{display:flex;align-items:center;gap:10px;padding:16px;border-top:1px solid #e9edf2}.profile>span{display:grid;width:34px;height:34px;place-items:center;color:#fff;background:#6266f2;border-radius:50%;font-weight:800}.profile strong,.profile small{display:block}.profile strong{font-size:11px}.profile small{margin-top:3px;color:#9299a5;font-size:9px}@media(max-width:760px){.sidebar{display:none}}
</style>
