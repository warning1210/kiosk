<template>
  <main class="home">
    <h1>Kiosk</h1>
    <p>아이스크림 프랜차이즈 키오스크 시스템</p>
    <RouterLink to="/kiosk/order">주문 시작하기</RouterLink>
    <RouterLink to="/admin/login">관리자 로그인</RouterLink>
  </main>
</template>

<script setup>
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 1rem;
}
</style>
