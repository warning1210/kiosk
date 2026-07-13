<template>
  <main class="size-page">
    <RouterLink :to="{ name: 'kiosk-type' }" class="back">‹ 주문 방법</RouterLink>
    <header>
      <p>STEP 2 · {{ orderTypeLabel }}</p>
      <h1>사이즈를 선택해 주세요</h1>
      <span>사이즈마다 선택할 수 있는 맛의 개수가 달라요</span>
    </header>
    <p v-if="loading" class="state">사이즈를 불러오는 중입니다.</p>
    <div v-else-if="error" class="state">사이즈 정보를 불러오지 못했습니다.</div>
    <section v-else class="size-grid">
      <button
        v-for="product in products"
        :key="product.productId"
        class="size-card"
        type="button"
        @click="selectProduct(product)"
      >
        <span class="cup" :class="`flavors-${product.selectableFlavorCount}`">{{ product.selectableFlavorCount }}</span>
        <strong>{{ product.productName }}</strong>
        <span>맛 {{ product.selectableFlavorCount }}개</span>
        <b>{{ product.basePrice.toLocaleString() }}원</b>
      </button>
    </section>
    <footer v-if="cart.itemCount" class="cart-bar">
      <button type="button" class="cart-info">
        <span class="bag">🛍️<b>{{ cart.itemCount }}</b></span>
        <span>{{ cart.itemCount }}개 상품</span>
      </button>
      <RouterLink :to="{ name: 'kiosk-checkout', query: { orderType } }" class="checkout">
        {{ cart.totalPrice.toLocaleString() }}원&nbsp;&nbsp; 결제하기 ›
      </RouterLink>
    </footer>
    <div v-if="promoProduct" class="promo-overlay" @click.self="promoProduct = null">
      <section class="promo-modal">
        <span>이달의 맛 프로모션</span>
        <h2>700원 추가하고<br>더블주니어로 바꿀까요?</h2>
        <p>이달의 맛 1개와 원하는 맛 1개,<br>총 2가지 맛을 즐길 수 있어요.</p>
        <div class="promo-price"><del>{{ promoProduct.basePrice.toLocaleString() }}원</del><strong>{{ (promoProduct.basePrice + 700).toLocaleString() }}원</strong></div>
        <div class="promo-actions"><button @click="chooseRegular">싱글레귤러 그대로</button><button class="promo-primary" @click="chooseUpgrade">+700원 업그레이드</button></div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import { useCartStore } from '../../stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const orderType = computed(() => route.query.orderType === 'TAKEOUT' ? 'TAKEOUT' : 'DINE_IN')
const orderTypeLabel = computed(() => orderType.value === 'TAKEOUT' ? '포장하기' : '먹고 가기')
const products = ref([])
const loading = ref(true)
const error = ref(false)
const promoProduct = ref(null)

function goToFlavors(product, extraQuery = {}) {
  router.push({ name: 'kiosk-order', query: { orderType: orderType.value, productId: product.productId, productName: product.productName, basePrice: product.basePrice, flavorCount: product.selectableFlavorCount, ...extraQuery } })
}
function selectProduct(product) {
  if (product.productName === '싱글레귤러') promoProduct.value = product
  else goToFlavors(product)
}
function chooseRegular() { const product = promoProduct.value; promoProduct.value = null; goToFlavors(product) }
function chooseUpgrade() {
  const single = promoProduct.value
  const doubleJunior = products.value.find((product) => product.productName === '더블주니어') || single
  promoProduct.value = null
  goToFlavors({ ...doubleJunior, productName: '더블주니어 이달의 맛 업그레이드', basePrice: single.basePrice + 700, selectableFlavorCount: 2 }, { monthlyRequired: '1' })
}

onMounted(async () => {
  try {
    const { data } = await http.get('/flavors/sizes')
    products.value = data
  } catch (requestError) {
    console.error(requestError)
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.size-page { min-height: 100vh; padding: 32px clamp(20px, 5vw, 70px) 130px; color: #392f34; background: #fff9fc; }
.back { color: #8e7480; text-decoration: none; }
header { margin: 34px 0; text-align: center; }
header p { color: #ef3f91; font-weight: 900; letter-spacing: .08em; }
h1 { margin: 8px; font-size: clamp(28px, 4.5vw, 46px); }
header span { color: #967e89; }
.size-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; width: min(1050px, 100%); margin: auto; }
.size-card { display: grid; min-height: 245px; padding: 24px 12px; place-content: center; justify-items: center; color: inherit; background: #fff; border: 2px solid #f2dce6; border-radius: 24px; font: inherit; cursor: pointer; transition: .18s; }
.size-card:hover { border-color: #ef3f91; box-shadow: 0 14px 30px rgb(239 63 145 / 14%); transform: translateY(-4px); }
.cup { display: grid; width: 88px; height: 88px; place-items: center; color: #fff; background: linear-gradient(145deg, #ef3f91, #f684b8); border-radius: 50% 50% 38% 38%; font-size: 30px; font-weight: 900; }
.size-card strong { margin-top: 18px; font-size: 20px; }
.size-card span:not(.cup) { margin: 5px; color: #8f7480; }
.size-card b { color: #ef3f91; font-size: 17px; }
.state { min-height: 40vh; text-align: center; }
.cart-bar { position: fixed; z-index: 5; right: 0; bottom: 0; left: 0; display: flex; justify-content: space-between; align-items: center; padding: 14px max(22px, calc((100vw - 1050px) / 2)); background: rgb(255 255 255 / 96%); border-top: 1px solid #ecdce4; box-shadow: 0 -12px 30px rgb(87 46 65 / 10%); }
.cart-info, .checkout { border: 0; background: transparent; cursor: pointer; }
.cart-info { display: flex; align-items: center; gap: 12px; color: #7c6670; }
.bag { position: relative; display: grid; width: 52px; height: 52px; place-items: center; border: 1px solid #eadce2; border-radius: 50%; font-size: 23px; }
.bag b { position: absolute; top: -6px; right: -3px; display: grid; width: 22px; height: 22px; place-items: center; color: #fff; background: #ef3f91; border-radius: 50%; font-size: 11px; }
.checkout { min-width: 210px; padding: 17px 24px; color: #fff; background: #ef3f91; border-radius: 999px; font-size: 16px; font-weight: 800; text-align: center; text-decoration: none; }
.promo-overlay { position: fixed; z-index: 10; inset: 0; display: grid; padding: 20px; place-items: center; background: rgb(42 32 37 / 58%); }
.promo-modal { width: min(480px, 100%); padding: 36px; text-align: center; background: #fff; border-radius: 28px; box-shadow: 0 24px 70px rgb(37 20 28 / 25%); }
.promo-modal > span { display: inline-block; padding: 6px 12px; color: #fff; background: #ef3f91; border-radius: 999px; font-size: 12px; font-weight: 900; }
.promo-modal h2 { margin: 18px 0 10px; font-size: 28px; }
.promo-modal p { color: #8f7883; line-height: 1.6; }
.promo-price { display: flex; justify-content: center; align-items: center; gap: 13px; margin: 20px; }
.promo-price del { color: #a4939a; }.promo-price strong { color: #ef3f91; font-size: 28px; }
.promo-actions { display: grid; grid-template-columns: 1fr 1.35fr; gap: 9px; }
.promo-actions button { padding: 16px 10px; border: 1px solid #efb6d0; color: #ef3f91; background: #fff; border-radius: 13px; font-weight: 800; cursor: pointer; }
.promo-actions .promo-primary { color: #fff; background: #ef3f91; border-color: #ef3f91; }
@media (max-width: 760px) { .size-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .size-card { min-height: 210px; } }
</style>
