<template>
  <div class="page">
    <!-- 상단 헤더: 로고 + 알림/닫기 -->
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <div class="top-actions">
        <!-- 언어 버튼을 누르면 아래 언어 선택 팝업을 엽니다. -->
        <button type="button" class="lang-pill" @click="showLanguageModal = true">
          <img :src="globe" alt="" class="lang-globe" />
          <span>{{ currentLanguage.nativeLabel }}</span>
          <img :src="chevron" alt="" class="lang-chevron" />
        </button>
        <button type="button" class="easy-mode-pill">{{ t('easyMode') }}</button>
        <button
          type="button"
          class="icon-btn notif-btn"
          :disabled="calling || justCalled"
          :aria-label="t('notification')"
          @click="callStaff"
        >
          <span class="notif-circle" v-html="notifCircleSvg"></span>
          <img :src="bell" class="bell-icon" alt="" />
        </button>
        <button type="button" class="icon-btn close-btn" :aria-label="t('goHome')" @click="orderFlow.goHome">
          <span v-html="closeXSvg"></span>
        </button>
      </div>
    </header>

    <Transition name="staff-call-toast-fade">
      <p v-if="justCalled" class="staff-call-toast">직원을 호출했어요. 잠시만 기다려주세요.</p>
    </Transition>

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
        {{ menuName(category.categoryName) }}
      </button>
    </nav>

    <!-- 상품 그리드: 맛 선택 화면과 동일하게 페이지 단위로 좌우 스와이프 -->
    <p v-if="orderFlow.loading" class="status-text">{{ t('loading') }}</p>
    <p v-else-if="orderFlow.loadError" class="status-text">{{ t('loadError') }}</p>
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
              :class="{ selected: focusedProduct?.productId === product.productId }"
              @click="onProductClick(product)"
            >
              <img v-if="product.imageUrl || productImage(product.productName)" :src="product.imageUrl || productImage(product.productName)" :alt="product.productName" class="product-image" />
              <div v-else class="product-image product-image--placeholder" />
              <p class="product-name">{{ menuName(product.productName) }}</p>
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
          :aria-label="`${page} ${t('page')}`"
          @click="currentPage = page - 1"
        ></button>
      </div>
    </template>

    <aside v-if="focusedProduct" class="product-description" :class="{ 'has-cart': cart.items.length }">
      <div>
        <strong>{{ menuName(focusedProduct.productName) }}</strong>
        <p>{{ locale === 'ko' && focusedProduct.description ? focusedProduct.description : t('defaultProductDescription') }}</p>
      </div>
      <button type="button" @click="addFocusedProduct">{{ t('addToCart') }}</button>
    </aside>

    <!-- 하단 바: 장바구니 / 결제 - 담긴 상품이 있을 때만 노출 -->
    <footer v-if="cart.items.length" class="bottom-bar">
      <button type="button" class="cart-btn" :aria-label="t('cart')" @click="goToCart">
        <span v-html="cartSvg"></span>
        <span class="cart-count-badge">{{ cart.totalCount }}</span>
      </button>
      <button type="button" class="checkout-btn" @click="goToCheckout">
        <span>{{ t('checkout') }}</span>
        <img :src="arrowForwardIos" alt="" class="checkout-arrow" />
      </button>
    </footer>

    <!-- 언어 선택 팝업: 목록을 수정하려면 useKioskI18n.js의 KIOSK_LANGUAGES만 바꾸면 됩니다. -->
    <div v-if="showLanguageModal" class="language-backdrop" @click.self="showLanguageModal = false">
      <section class="language-modal" role="dialog" aria-modal="true" :aria-label="t('selectLanguage')">
        <h2>{{ t('selectLanguage') }}</h2>
        <div class="language-options">
          <button
            v-for="language in KIOSK_LANGUAGES"
            :key="language.locale"
            type="button"
            class="language-option"
            :class="{ selected: locale === language.locale }"
            @click="selectLanguage(language.locale)"
          >
            <span>{{ language.nativeLabel }}</span>
            <small>{{ language.label }}</small>
          </button>
        </div>
        <button type="button" class="language-close" @click="showLanguageModal = false">{{ t('close') }}</button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import { useStaffCall } from '../../../composables/useStaffCall'

import logo from '../../../assets/kiosk/logo.png'
import globe from '../../../assets/kiosk/icons/globe.png'
import chevron from '../../../assets/kiosk/icons/chevron.png'
import bell from '../../../assets/kiosk/icons/bell.png'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import cartRaw from '../../../assets/kiosk/icons/cart.svg?raw'
import notifCircleRaw from '../../../assets/kiosk/icons/notif-circle.svg?raw'
import { productImage } from '../../../data/productImages'
import { KIOSK_LANGUAGES, useKioskI18n } from '../../../composables/useKioskI18n'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
const focusedProduct = ref(null)
const { calling, justCalled, callStaff } = useStaffCall()
const showLanguageModal = ref(false)

// 번역 함수와 현재 언어는 공통 composable에서 가져옵니다.
const { locale, currentLanguage, setLocale, t, menuName } = useKioskI18n()

// 언어를 고르면 즉시 문구를 바꾸고 팝업을 닫습니다.
function selectLanguage(nextLocale) {
  setLocale(nextLocale)
  showLanguageModal.value = false
}

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
  focusedProduct.value = null
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
  if (!product.requiresFlavorSelection && !orderFlow.needsContainerStep(product)) {
    focusedProduct.value = product
    return
  }
  orderFlow.selectProduct(product)
}

function addFocusedProduct() {
  if (!focusedProduct.value) return
  orderFlow.editingItemId = null
  orderFlow.resetFlavorStepState(focusedProduct.value)
  orderFlow.addCurrentSelectionToCart()
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  width: 100%;
  margin: 0 auto;
  padding-bottom: 194px;
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 130px;
  padding: 0 27px;
  flex-shrink: 0;
}

.logo {
  height: 100px;
  width: 112px;
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
  height: 44px;
  padding: 0 18px;
  border-radius: 22px;
  font-size: 24px;
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
  width: 62px;
  height: 62px;
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
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.staff-call-toast {
  position: fixed;
  top: 24px;
  left: 50%;
  z-index: 60;
  padding: 14px 22px;
  color: #fff;
  background: rgb(20 20 20 / 85%);
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  transform: translateX(-50%);
}

.staff-call-toast-fade-enter-active,
.staff-call-toast-fade-leave-active {
  transition: opacity 0.3s ease;
}

.staff-call-toast-fade-enter-from,
.staff-call-toast-fade-leave-to {
  opacity: 0;
}

.close-btn :deep(svg) {
  width: 64px;
  height: 64px;
}

.category-tabs {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 124px;
  padding: 0 18px;
  overflow-x: auto;
  flex-shrink: 0;
}

.category-tab {
  flex-shrink: 0;
  border: none;
  background: transparent;
  font-size: 30px;
  font-weight: 500;
  color: #f20c93;
  cursor: pointer;
  padding: 14px 32px;
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
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  touch-action: pan-y;
  user-select: none;
  cursor: grab;
}

.product-track {
  display: flex;
  height: 100%;
  transition: transform 0.35s ease;
}

.product-grid {
  flex: 0 0 100%;
  min-width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-content: start;
  row-gap: 22px;
  padding: 24px 0 0;
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
  justify-content: center;
  height: auto;
  border: none;
  background: #fff;
  cursor: pointer;
  padding: 16px 8px;
}

.product-card.selected {
  border: 2px solid #f20c93;
  border-radius: 16px;
  background: #fff7fb;
}

.product-image {
  width: 160px;
  height: 144px;
  object-fit: contain;
  margin-top: 0;
}

.product-image--placeholder {
  background: #f4f4f4;
  border-radius: 8px;
}

.product-name {
  margin: 16px 0 0;
  font-size: 26px;
  line-height: 1.25;
  word-break: keep-all;
  color: #000;
}

.product-price {
  margin: 8px 0 0;
  font-size: 26px;
  color: #f20c93;
}

.product-description {
  position: fixed;
  bottom: 20px;
  left: 50%;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 18px;
  width: calc(100% - 32px);
  max-width: 992px;
  padding: 14px 18px;
  transform: translateX(-50%);
  box-sizing: border-box;
  border: 1px solid #f0b8d5;
  border-radius: 12px;
  background: #fff5fa;
  box-shadow: 0 8px 22px rgb(94 50 69 / 10%);
}

.product-description.has-cart { bottom: 210px; }
.product-description div { min-width: 0; flex: 1; }
.product-description strong { display: block; overflow: hidden; color: #f20c93; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.product-description p { overflow: hidden; margin: 5px 0 0; color: #5f5057; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.product-description button { flex: 0 0 auto; padding: 13px 24px; border: 0; border-radius: 999px; color: #fff; background: #f20c93; font-size: 17px; font-weight: 700; cursor: pointer; }

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

/* 언어 선택 팝업은 키오스크의 다른 확인 팝업과 같은 느낌으로 구성합니다. */
.language-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgb(0 0 0 / 45%);
}

.language-modal {
  width: min(620px, 92vw);
  padding: 34px;
  border-radius: 26px;
  background: #fff;
  text-align: center;
  box-shadow: 0 18px 55px rgb(0 0 0 / 18%);
}

.language-modal h2 {
  margin: 0 0 24px;
  font-size: 28px;
  color: #222;
}

.language-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.language-option {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 20px 16px;
  border: 2px solid #dedede;
  border-radius: 16px;
  background: #fff;
  color: #222;
  font-size: 22px;
  cursor: pointer;
}

.language-option small {
  color: #969696;
  font-size: 13px;
}

.language-option.selected {
  border-color: #f20c93;
  background: #fff5fa;
  color: #f20c93;
}

.language-close {
  width: 100%;
  height: 58px;
  margin-top: 22px;
  border: 0;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 20px;
  cursor: pointer;
}
</style>
