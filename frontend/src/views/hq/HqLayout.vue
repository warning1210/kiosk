<template>
  <!-- 본점 관리 화면들이 함께 사용하는 사이드바 + 본문 레이아웃 -->
  <div class="layout">
    <aside class="sidebar">
      <!-- 본점 시스템을 식별하는 공통 상단 정보 -->
      <div class="brand">
        <strong>본점 관리 시스템</strong>
        <span>Kiosk Admin</span>
      </div>
      <!-- hq-stock-requests라는 라우트 이름이 본점 재고신청 목록 경로와 연결된다. -->
      <nav>
        <span class="nav-section">메뉴</span>
        <router-link class="nav-item disabled" to="#">대시보드</router-link>
        <router-link class="nav-item" :to="{ name: 'hq-stock-requests' }">재고 신청</router-link>
        <router-link class="nav-item disabled" to="#">재고 현황</router-link>
        <router-link class="nav-item disabled" to="#">분점 관리</router-link>
        <router-link class="nav-item disabled" to="#">매출 관리</router-link>
        <router-link class="nav-item disabled" to="#">이벤트 관리</router-link>
      </nav>
      <!-- 현재 선택된 관리자와 표시용 역할, 관리자 전환 동작 -->
      <div class="actor">
        <div class="avatar">{{ initial }}</div>
        <div>
          <strong>{{ devActor.admin?.name }}</strong>
          <span>본점 · {{ roleLabel }}</span>
        </div>
        <button type="button" class="switch" @click="switchAdmin">전환</button>
      </div>
    </aside>
    <!-- 현재 /hq 하위 경로가 선택한 자식 화면을 이 위치에 렌더링한다. -->
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDevActorStore } from '../../stores/devActor'

// 현재 관리자 정보는 Pinia 저장소에서 읽고, 화면 이동은 router가 담당한다.
const devActor = useDevActorStore()
const router = useRouter()

// 원본 관리자 정보에서 표시용 값을 계산한다. 실제 역할별 API 권한은 백엔드가 검사한다.
const initial = computed(() => devActor.admin?.name?.[0] ?? '?')
const roleLabel = computed(() => (devActor.admin?.role === 'SUPER_ADMIN' ? '슈퍼관리자' : '본점 관리자'))

// 관리자 정보를 비운 후 선택 화면으로 돌아가 다른 역할로 진입할 수 있게 한다.
function switchAdmin() {
  devActor.clear()
  router.push({ name: 'admin-select' })
}
</script>

<style scoped>
/* 220px 기준 너비의 사이드바와 남은 공간을 쓰는 본문을 가로 배치한다. */
.layout {
  display: flex;
  min-height: 100vh;
  background: #f7f7fb;
}

.sidebar {
  width: 220px;
  background: white;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 1rem 0.75rem;
}

/* 본점 시스템 이름 */
.brand {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0.5rem 1rem;
  border-bottom: 1px solid #f1f1f4;
  margin-bottom: 0.75rem;
}

.brand strong {
  font-size: 1rem;
}

.brand span {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* 세로 메뉴와 현재 라우트의 활성 상태 */
nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.nav-section {
  font-size: 0.7rem;
  color: #9ca3af;
  padding: 0.5rem;
}

.nav-item {
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  color: #374151;
  text-decoration: none;
  font-size: 0.875rem;
}

.nav-item:hover {
  background: #f1f1f8;
}

.nav-item.router-link-active {
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 600;
}

.nav-item.disabled {
  color: #d1d5db;
  pointer-events: none;
}

/* 선택한 관리자 정보와 전환 버튼 */
.actor {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 0.5rem 0.25rem;
  border-top: 1px solid #f1f1f4;
}

.actor > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.actor strong {
  font-size: 0.8125rem;
}

.actor span {
  font-size: 0.7rem;
  color: #9ca3af;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #4f46e5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8125rem;
  flex-shrink: 0;
}

.switch {
  border: none;
  background: transparent;
  color: #4f46e5;
  font-size: 0.75rem;
  cursor: pointer;
}

/* router-view로 들어오는 실제 자식 화면 영역 */
.content {
  flex: 1;
  padding: 1.5rem;
  min-width: 0;
}
</style>
