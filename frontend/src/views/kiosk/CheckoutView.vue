<template>
  <main class="checkout-page">
    <header><span>STEP 3</span><h1>주문 내역을 확인해 주세요</h1></header>
    <section class="order-list">
      <article v-for="(item, index) in cart.items" :key="item.cartItemId">
        <img :src="item.flavors[0]?.imageUrl" :alt="item.productName">
        <div><strong>{{ item.productName }}</strong><p>{{ item.flavors.map(f => f.flavorName).join(', ') }}</p></div>
        <b>{{ item.basePrice.toLocaleString() }}원</b>
        <button type="button" @click="cart.removeItem(item.cartItemId)">×</button>
      </article>
    </section>
    <RouterLink :to="{ name: 'kiosk-size', query: { orderType } }" class="more">메뉴 더 담으러 가기</RouterLink>
    <section class="summary"><span>총 주문금액</span><strong>{{ cart.totalPrice.toLocaleString() }}원</strong></section>
    <section class="method"><h2>결제 방법을 선택해 주세요</h2><div><button @click="cashModal = true">현금</button><button class="primary" @click="goCard">신용카드</button></div></section>

    <div v-if="cashModal" class="overlay" @click.self="cashModal = false">
      <div class="modal"><span class="cash-icon">₩</span><h2>카운터에서 결제를 도와드릴게요</h2><p>직원에게 주문 내용을 말씀해 주세요.</p><div><button @click="cashModal = false">취소</button><button class="primary" @click="finishCash">확인</button></div></div>
    </div>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
const route = useRoute(); const router = useRouter(); const cart = useCartStore(); const cashModal = ref(false)
const orderType = computed(() => route.query.orderType === 'TAKEOUT' ? 'TAKEOUT' : 'DINE_IN')
function goCard() { if (cart.itemCount) router.push({ name: 'kiosk-benefits', query: { orderType: orderType.value } }) }
function finishCash() { router.push({ name: 'kiosk-receipt', query: { method: 'CASH', orderType: orderType.value } }) }
</script>

<style scoped>
.checkout-page{min-height:100vh;padding:32px max(22px,calc((100vw - 850px)/2)) 60px;color:#392f34;background:#fffafd}header{text-align:center;margin-bottom:28px}header span{color:#ef3f91;font-weight:900}h1{margin:8px}.order-list{background:#fff;border:1px solid #eedfe6;border-radius:24px;overflow:hidden}.order-list article{display:grid;grid-template-columns:74px 1fr auto 36px;gap:16px;align-items:center;padding:16px;border-bottom:1px solid #f2e7ec}.order-list article:last-child{border:0}.order-list img{width:68px;height:68px;object-fit:contain}.order-list p{margin:5px 0;color:#917985}.order-list button{border:0;background:none;font-size:24px;color:#9c8991;cursor:pointer}.more{display:block;margin:24px 0;padding:15px;text-align:center;color:#ef3f91;border:1px solid #efb8d1;border-radius:999px;text-decoration:none}.summary{display:flex;justify-content:space-between;padding:24px 4px;font-size:20px}.summary strong{color:#ef3f91}.method{padding-top:20px;border-top:8px solid #f5edf1}.method h2{text-align:center}.method>div,.modal>div{display:grid;grid-template-columns:1fr 1fr;gap:12px}.method button,.modal button{padding:18px;border:1px solid #ef3f91;color:#ef3f91;background:#fff;border-radius:12px;font-size:18px;font-weight:800;cursor:pointer}.primary{color:#fff!important;background:#ef3f91!important}.overlay{position:fixed;inset:0;display:grid;place-items:center;padding:20px;background:rgb(35 27 31/55%)}.modal{width:min(430px,100%);padding:34px;text-align:center;background:#fff;border-radius:24px}.cash-icon{display:grid;width:68px;height:68px;margin:auto;place-items:center;color:#fff;background:#ef3f91;border-radius:50%;font-size:34px;font-weight:900}.modal p{color:#8f7983;margin-bottom:28px}@media(max-width:560px){.order-list article{grid-template-columns:58px 1fr 30px}.order-list b{grid-column:2}.order-list img{width:54px;height:54px}}
</style>
