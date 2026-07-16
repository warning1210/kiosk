<template>
  <div class="page">
    <!-- 상단 헤더: 로고 + 알림/닫기 -->
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <div class="top-actions">
        <button type="button" class="lang-pill">
          <img :src="globe" alt="" class="lang-globe" />
          <span>한국어</span>
          <img :src="chevron" alt="" class="lang-chevron" />
        </button>
        <button type="button" class="easy-mode-pill">쉬운 모드</button>
        <button type="button" class="icon-btn notif-btn" aria-label="알림">
          <span class="notif-circle" v-html="notifCircleSvg"></span>
          <img :src="bell" class="bell-icon" alt="" />
        </button>
        <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="goHome">
          <span v-html="closeXSvg"></span>
        </button>
      </div>
    </header>

    <!-- 카테고리 탭 -->
    <nav class="category-tabs">
      <button
        v-for="category in orderFlow.categories"
        :key="category.categoryId"
        type="button"
        class="category-tab"
        :class="{ active: orderFlow.selectedCategory?.categoryId === category.categoryId }"
        @click="orderFlow.selectedCategory = category"
      >
        {{ category.categoryName }}
      </button>
    </nav>

    <!-- 상품 그리드 -->
    <p v-if="orderFlow.loading" class="status-text">불러오는 중...</p>
    <p v-else-if="orderFlow.loadError" class="status-text">상품을 불러오지 못했습니다.</p>
    <div v-else class="product-grid">
      <button
        v-for="product in orderFlow.visibleProducts"
        :key="product.productId"
        type="button"
        class="product-card"
        @click="orderFlow.selectProduct(product)"
      >
        <img v-if="productImage(product.productName)" :src="productImage(product.productName)" :alt="product.productName" class="product-image" />
        <div v-else class="product-image product-image--placeholder" />
        <p class="product-name">{{ product.productName }}</p>
        <p class="product-price">₩{{ product.basePrice.toLocaleString() }}</p>
      </button>
    </div>

    <!-- 하단 바: 장바구니 / 결제 -->
    <footer class="bottom-bar">
      <button type="button" class="cart-btn" aria-label="장바구니" @click="goToCart">
        <span v-html="cartSvg"></span>
      </button>
      <button type="button" class="checkout-btn" :disabled="!cart.items.length" @click="goToCart">
        <span>결제하기</span>
        <img :src="arrowForwardIos" alt="" class="checkout-arrow" />
      </button>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'

import logo from '../../../assets/kiosk/logo.png'
import globe from '../../../assets/kiosk/icons/globe.png'
import chevron from '../../../assets/kiosk/icons/chevron.png'
import bell from '../../../assets/kiosk/icons/bell.png'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import cartRaw from '../../../assets/kiosk/icons/cart.svg?raw'
import notifCircleRaw from '../../../assets/kiosk/icons/notif-circle.svg?raw'

import singleRegular from '../../../assets/kiosk/products/single-regular.png'
import singleKing from '../../../assets/kiosk/products/single-king.png'
import doubleJunior from '../../../assets/kiosk/products/double-junior.png'
import doubleRegular from '../../../assets/kiosk/products/double-regular.png'
import pint from '../../../assets/kiosk/products/pint.png'
import quart from '../../../assets/kiosk/products/quart.png'
import family from '../../../assets/kiosk/products/family.png'
import halfGallon from '../../../assets/kiosk/products/half-gallon.png'

const router = useRouter()
const orderFlow = useOrderFlowStore()
const cart = useCartStore()

const closeXSvg = closeXRaw
const cartSvg = cartRaw
const notifCircleSvg = notifCircleRaw

// Figma 목업의 컵/콘 아이콘을 실제 상품명(seed 데이터 기준)에 매칭
const PRODUCT_IMAGES = {
  싱글레귤러: singleRegular,
  싱글킹: singleKing,
  더블주니어: doubleJunior,
  더블레귤러: doubleRegular,
  파인트: pint,
  쿼터: quart,
  패밀리: family,
  하프갤런: halfGallon
}

function productImage(productName) {
  return PRODUCT_IMAGES[productName] ?? null
}

function goToCart() {
  if (!cart.items.length) return
  orderFlow.step = 'cart'
}

// CU-014: 홈버튼과 동일하게, 진행 중인 주문이 있으면 확인 후 초기 화면으로 복귀
function goHome() {
  if (cart.items.length > 0 && !confirm('진행 중인 주문을 취소하고 처음 화면으로 돌아가시겠습니까?')) return
  orderFlow.stopPolling()
  cart.clear()
  router.push('/')
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  margin: 0 auto;
  padding-bottom: 194px;
  background: #fff;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 114px;
  padding: 0 27px;
}

.logo {
  height: 88px;
  width: 96px;
  object-fit: contain;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lang-pill,
.easy-mode-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border-radius: 20px;
  font-size: 20px;
  font-weight: 500;
  cursor: pointer;
}

.lang-pill {
  border: 1px solid #767676;
  background: #fff;
  color: #000;
}

.lang-globe {
  width: 20px;
  height: 20px;
}

.lang-chevron {
  width: 12px;
  height: 12px;
  transform: rotate(90deg);
}

.easy-mode-pill {
  border: 1px solid #000;
  background: #2d49ff;
  color: #fff;
}

.icon-btn {
  position: relative;
  width: 53px;
  height: 53px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.notif-circle {
  position: absolute;
  inset: 0;
}

.bell-icon {
  position: relative;
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.close-btn :deep(svg) {
  width: 55px;
  height: 55px;
}

.category-tabs {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100px;
  padding: 0 14px;
  overflow-x: auto;
}

.category-tab {
  flex-shrink: 0;
  border: none;
  background: transparent;
  font-size: 20px;
  font-weight: 500;
  color: #f20c93;
  cursor: pointer;
  padding: 11px 24px;
  border-radius: 99px;
}

.category-tab.active {
  background: #f20c93;
  color: #fff;
  font-family: 'Mochiy Pop P One', sans-serif;
}

.status-text {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.product-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 164px;
  border: none;
  background: #fff;
  cursor: pointer;
  padding: 0;
}

.product-image {
  width: 110px;
  height: 97px;
  object-fit: contain;
  margin-top: 0;
}

.product-image--placeholder {
  background: #f4f4f4;
  border-radius: 8px;
}

.product-name {
  margin: 8px 0 0;
  font-size: 17px;
  color: #000;
}

.product-price {
  margin: 4px 0 0;
  font-size: 17px;
  color: #f20c93;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  height: 194px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.cart-btn {
  width: 202px;
  height: 101px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.cart-btn :deep(svg) {
  width: 100%;
  height: 100%;
}

.checkout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 385px;
  height: 101px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #f20c93;
  color: #fff;
  font-size: 30px;
  cursor: pointer;
}

.checkout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.checkout-arrow {
  width: 14px;
  height: 24px;
}
</style>
