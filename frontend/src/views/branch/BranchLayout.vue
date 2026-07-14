<template>
  <!-- 지점 관리 화면들이 함께 사용하는 사이드바 + 본문 레이아웃 -->
  <div class="layout">
    <aside class="sidebar">
      <!-- 현재 선택한 지점 이름을 공통 상단 정보로 보여 준다. -->
      <div class="brand">
        <strong>{{ devActor.admin?.branchName ?? '지점' }}</strong>
        <span>매장 관리 시스템</span>
      </div>
      <!--
        router-link의 :to 객체에 넣은 name은 router/index.js에 정의한 자식 라우트 이름을 가리킨다.
        재고 현황에서 신청 모달을 열 수 있고, 입고 신청 현황에서 이후 상태를 확인한다.
      -->
      <nav>
        <span class="nav-section">메뉴</span>
        <router-link class="nav-item disabled" to="#">대시보드</router-link>
        <router-link class="nav-item disabled" to="#">주문 관리</router-link>
        <router-link class="nav-item" :to="{ name: 'branch-inventory' }">재고 현황</router-link>
        <router-link class="nav-item" :to="{ name: 'branch-stock-requests' }">입고 신청 현황</router-link>
        <router-link class="nav-item disabled" to="#">이벤트 관리</router-link>
        <router-link class="nav-item disabled" to="#">판매 통계</router-link>
        <router-link class="nav-item disabled" to="#">매장 설정</router-link>
      </nav>
      <!-- 현재 관리자 정보와 다른 관리자로 전환하는 동작 -->
      <div class="actor">
        <div class="avatar">{{ initial }}</div>
        <div>
          <strong>{{ devActor.admin?.name }}</strong>
          <span>{{ devActor.admin?.branchName }} · 점장</span>
        </div>
        <button type="button" class="switch" @click="switchAdmin">전환</button>
      </div>
    </aside>
    <!-- 현재 /branch 하위 경로에 맞는 자식 컴포넌트가 이 위치에 들어온다. -->
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDevActorStore } from '../../stores/devActor'

// Pinia 저장소에서는 현재 관리자 정보를, router에서는 화면 이동 기능을 가져온다.
const devActor = useDevActorStore()
const router = useRouter()

// 관리자 이름이 바뀌면 사이드바 아바타의 첫 글자도 자동으로 다시 계산된다.
const initial = computed(() => devActor.admin?.name?.[0] ?? '?')

// 기존 관리자 선택을 지운 뒤 선택 화면으로 이동해 다른 역할/지점으로 전환한다.
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

/* 지점 이름과 시스템 설명 */
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
  background: #6366f1;
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
  color: #6366f1;
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
