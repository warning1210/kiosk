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
        <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="orderFlow.goHome">
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

    <!-- 상품 그리드: 맛 선택 화면과 동일하게 페이지 단위로 좌우 스와이프 -->
    <p v-if="orderFlow.loading" class="status-text">불러오는 중...</p>
    <p v-else-if="orderFlow.loadError" class="status-text">상품을 불러오지 못했습니다.</p>
    <template v-else>
      <div
        class="product-viewport"
        @pointerdown="onSwipeStart"
        @pointermove="onSwipeMove"
        @pointerup="onSwipeEnd"
        @pointercancel="onSwipeEnd"
      >
        <div class="product-track" :style="{ transform: `translateX(-${currentPage * 100}%)` }">
          <div v-for="(page, pageIndex) in productPages" :key="pageIndex" class="product-grid">
            <button
              v-for="product in page"
              :key="product.productId"
              type="button"
              class="product-card"
              @click="onProductClick(product)"
            >
              <img v-if="productImage(product.productName)" :src="productImage(product.productName)" :alt="product.productName" class="product-image" />
              <div v-else class="product-image product-image--placeholder" />
              <p class="product-name">{{ product.productName }}</p>
              <p class="product-price">₩{{ product.basePrice.toLocaleString() }}</p>
            </button>
          </div>
        </div>
      </div>

      <!-- 페이지 인디케이터 -->
      <div v-if="totalProductPages > 1" class="page-dots">
        <button
          v-for="page in totalProductPages"
          :key="page"
          type="button"
          class="page-dot"
          :class="{ active: currentPage === page - 1 }"
          :aria-label="`${page}페이지`"
          @click="currentPage = page - 1"
        ></button>
      </div>
    </template>

    <!-- 하단 바: 장바구니 / 결제 - 담긴 상품이 있을 때만 노출 -->
    <footer v-if="cart.items.length" class="bottom-bar">
      <button type="button" class="cart-btn" aria-label="장바구니" @click="goToCart">
        <span v-html="cartSvg"></span>
        <span class="cart-count-badge">{{ cart.totalCount }}</span>
      </button>
      <button type="button" class="checkout-btn" @click="goToCheckout">
        <span>결제하기</span>
        <img :src="arrowForwardIos" alt="" class="checkout-arrow" />
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
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
import { productImage } from '../../../data/productImages'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()

const closeXSvg = closeXRaw
const cartSvg = cartRaw
const notifCircleSvg = notifCircleRaw

function goToCart() {
  if (!cart.items.length) return
  orderFlow.step = 'cart'
}

function goToCheckout() {
  if (!cart.items.length) return
  orderFlow.step = 'customer'
}

// 상품 그리드도 맛 선택과 동일한 방식으로 페이지 단위 좌우 스와이프
const PRODUCTS_PER_PAGE = 16 // 4열 x 4행
const SWIPE_THRESHOLD = 40 // px

const currentPage = ref(0)
const productPages = computed(() => {
  const pages = []
  const items = orderFlow.visibleProducts
  for (let i = 0; i < items.length; i += PRODUCTS_PER_PAGE) {
    pages.push(items.slice(i, i + PRODUCTS_PER_PAGE))
  }
  return pages.length ? pages : [[]]
})
const totalProductPages = computed(() => productPages.value.length)

// 카테고리를 바꾸면 이전 카테고리에서 보던 페이지 위치가 아니라 첫 페이지부터 보여준다
watch(() => orderFlow.selectedCategory, () => {
  currentPage.value = 0
})

let swipeStartX = null
const isDragging = ref(false)

function onSwipeStart(e) {
  swipeStartX = e.clientX
}

function onSwipeMove(e) {
  if (swipeStartX === null) return
  isDragging.value = Math.abs(e.clientX - swipeStartX) > 10
}

function onSwipeEnd(e) {
  if (swipeStartX === null) return
  const delta = e.clientX - swipeStartX
  swipeStartX = null
  if (Math.abs(delta) < SWIPE_THRESHOLD) {
    isDragging.value = false
    return
  }
  if (delta < 0 && currentPage.value < totalProductPages.value - 1) currentPage.value += 1
  if (delta > 0 && currentPage.value > 0) currentPage.value -= 1
  requestAnimationFrame(() => { isDragging.value = false })
}

function onProductClick(product) {
  if (isDragging.value) return
  orderFlow.selectProduct(product)
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  margin: 0 auto;
  padding-bottom: 194px;
  background: #fff;
  min-height: 100vh;
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

.product-viewport {
  overflow: hidden;
  touch-action: pan-y;
  user-select: none;
  cursor: grab;
}

.product-track {
  display: flex;
  transition: transform 0.35s ease;
}

.product-grid {
  flex: 0 0 100%;
  min-width: 100%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.page-dots {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 16px 0;
}

.page-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: #d9d9d9;
  padding: 0;
  cursor: pointer;
}

.page-dot.active {
  background: #f20c93;
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
  position: relative;
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

.cart-count-badge {
  position: absolute;
  top: 14px;
  right: 70px;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
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
