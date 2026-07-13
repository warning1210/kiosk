<template>
  <main class="choice-page">
    <RouterLink to="/" class="back">‹ 처음으로</RouterLink>
    <header>
      <p>STEP 1</p>
      <h1>어디에서 드시나요?</h1>
      <span>주문 방법을 선택해 주세요</span>
    </header>
    <section class="choices">
      <button type="button" @click="startOrder('DINE_IN')">
        <span class="icon">🍨</span>
        <strong>먹고 가기</strong>
        <small>매장에서 드실게요</small>
      </button>
      <button type="button" @click="startOrder('TAKEOUT')">
        <span class="icon">🛍️</span>
        <strong>포장하기</strong>
        <small>가지고 갈게요</small>
      </button>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const cart = useCartStore()

function startOrder(orderType) {
  cart.clear()
  router.push({ name: 'kiosk-size', query: { orderType } })
}
</script>

<style scoped>
.choice-page { min-height: 100vh; padding: 36px clamp(24px, 6vw, 80px); color: #382f33; background: #fff9fc; }
.back { color: #8e7480; text-decoration: none; }
header { margin: 8vh 0 50px; text-align: center; }
header p { margin: 0; color: #ef3f91; font-weight: 900; letter-spacing: .14em; }
h1 { margin: 10px 0; font-size: clamp(30px, 5vw, 50px); }
header span { color: #987f8a; font-size: 18px; }
.choices { display: grid; grid-template-columns: repeat(2, minmax(240px, 360px)); justify-content: center; gap: 28px; }
.choices button { display: grid; min-height: 310px; padding: 38px; place-content: center; justify-items: center; color: inherit; background: #fff; border: 3px solid #f5dbe7; border-radius: 32px; box-shadow: 0 18px 45px rgb(120 67 91 / 10%); font: inherit; cursor: pointer; transition: .2s; }
.choices button:hover { border-color: #ef3f91; transform: translateY(-6px); }
.icon { font-size: 90px; }
strong { margin-top: 24px; font-size: 30px; }
small { margin-top: 9px; color: #9c8490; font-size: 15px; }
@media (max-width: 620px) { .choices { grid-template-columns: 1fr 1fr; gap: 12px; } .choices button { min-width: 0; min-height: 240px; padding: 20px 8px; } .icon { font-size: 60px; } strong { font-size: 22px; } }
</style>
