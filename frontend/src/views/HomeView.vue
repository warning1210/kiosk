<template>
  <!-- CU-001: 광고 화면을 터치(클릭)하면 주문 프로세스가 시작된다 -->
  <main class="home" @click="startOrder">
    <h1>Kiosk</h1>
    <p>아이스크림 프랜차이즈 키오스크 시스템</p>
    <p>화면을 터치하여 시작하세요 / Touch to start</p>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cart = useCartStore()

function startOrder() {
  // 이전 고객의 장바구니가 다음 고객에게 이어지면 안 되므로 새 주문 시작 시 초기화
  cart.clear()
  router.push('/kiosk/order')
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 1rem;
  cursor: pointer;
}
</style>
