<template>
  <main class="payment-page">
    <header><span>STEP 5</span><h1>결제수단을 선택해 주세요</h1></header>
    <div v-if="route.query.timeout" class="timeout">결제 시간이 초과되었습니다. 결제수단을 다시 선택해 주세요.</div>
    <section class="methods">
      <button @click="router.push({name:'kiosk-toss-qr',query:{orderType:route.query.orderType||'DINE_IN'}})"><b class="toss">toss</b><strong>토스페이 QR 결제</strong><span>QR을 스캔하여 결제</span></button>
      <button @click="completeCard"><b>💳</b><strong>신용카드 결제</strong><span>카드를 삽입해 주세요</span></button>
    </section>
    <section class="amount"><span>최종 결제금액</span><strong>{{ cart.finalPrice.toLocaleString() }}원</strong></section>
    <button class="back" @click="router.back()">이전</button>
  </main>
</template>
<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
const route=useRoute();const router=useRouter();const cart=useCartStore()
function completeCard(){router.push({name:'kiosk-receipt',query:{method:'CARD',orderType:route.query.orderType||'DINE_IN'}})}
</script>
<style scoped>
.payment-page{min-height:100vh;padding:38px max(20px,calc((100vw - 800px)/2));color:#382f33;background:#fffafd}header{text-align:center}header span{color:#ef3f91;font-weight:900}h1{margin:9px}.timeout{margin:28px 0;padding:16px;text-align:center;color:#c52b68;background:#ffe5f0;border-radius:12px}.methods{display:grid;grid-template-columns:1fr 1fr;gap:22px;margin:50px 0}.methods button{display:grid;min-height:270px;padding:30px;place-content:center;justify-items:center;border:2px solid #efdae4;background:#fff;border-radius:28px;cursor:pointer;transition:.2s}.methods button:hover{border-color:#ef3f91;transform:translateY(-5px)}.methods b{display:grid;min-width:72px;height:72px;padding:0 12px;place-items:center;background:#fceaf2;border-radius:20px;font-size:35px}.methods .toss{color:#fff;background:#1261ff;font-size:25px;font-style:italic}.methods strong{margin:20px 0 7px;font-size:21px}.methods span{color:#95808a}.amount{display:flex;justify-content:space-between;padding:22px;background:#fff;border-radius:18px}.amount strong{color:#ef3f91;font-size:24px}.back{width:100%;margin-top:20px;padding:16px;border:1px solid #eab7cf;color:#ef3f91;background:#fff;border-radius:999px}@media(max-width:560px){.methods{gap:10px}.methods button{min-height:220px;padding:15px}.methods strong{font-size:17px}}
</style>
