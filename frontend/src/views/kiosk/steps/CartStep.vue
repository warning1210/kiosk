<template>
  <!-- 6단계: 장바구니 확인 (CU-007) -->
  <div class="page">
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <div class="top-actions">
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

    <div class="step-row">
      <span class="step-pill">
        <span v-html="stepPillSvg"></span>
        <span class="step-pill-text">STEP01</span>
      </span>
      <span class="step-title">{{ t('reviewOrder') }}</span>
    </div>

    <p v-if="!cart.items.length" class="empty-cart">장바구니가 비어있습니다. 메뉴를 담아주세요.</p>
    <ul v-else class="cart-list">
      <li v-for="item in cart.items" :key="item.id" class="cart-row">
        <img v-if="item.imageUrl || productImage(item.productName)" :src="item.imageUrl || productImage(item.productName)" :alt="item.productName" class="cart-thumb" />
        <div v-else class="cart-thumb cart-thumb--placeholder" />
        <div class="cart-item-text">
          <p class="cart-item-name">{{ menuName(item.productName) }}</p>
          <p class="cart-item-detail">{{ itemDetail(item) }}</p>
        </div>
        <div class="cart-item-price">
          <span v-if="item.originalUnitPrice > item.unitPrice" class="price-original">
            ₩{{ (item.originalUnitPrice * item.quantity).toLocaleString() }}
          </span>
          <span class="price-final">₩{{ (item.unitPrice * item.quantity).toLocaleString() }}</span>
        </div>
        <div class="quantity-control" :aria-label="t('quantity')">
          <button
            type="button"
            class="quantity-btn"
            :disabled="item.quantity <= 1"
            :aria-label="`${item.productName} 수량 줄이기`"
            @click="cart.adjustQuantity(item.id, -1)"
          >−</button>
          <span class="cart-item-qty">{{ item.quantity }}</span>
          <button
            type="button"
            class="quantity-btn"
            :aria-label="`${item.productName} 수량 늘리기`"
            @click="cart.adjustQuantity(item.id, 1)"
          >+</button>
        </div>
        <button v-if="canEditItem(item)" type="button" class="row-icon-btn" :aria-label="t('edit')" @click="orderFlow.editItem(item)">
          <span v-html="editPencilSvg"></span>
        </button>
        <button type="button" class="row-icon-btn" :aria-label="t('delete')" @click="orderFlow.removeFromCart(item.id)">
          <span v-html="deleteXSvg"></span>
        </button>
      </li>
    </ul>

    <button type="button" class="add-more-btn" @click="orderFlow.step = 'product'">{{ t('addMore') }}</button>

    <footer class="pay-section">
      <div class="pay-header">
        <span class="step-pill">
          <span v-html="stepPillSvg"></span>
          <span class="step-pill-text">STEP02</span>
        </span>
        <span class="step-title">{{ t('selectPayment') }}</span>
        <span class="pay-total">₩{{ cart.amountBeforeDiscount.toLocaleString() }}</span>
      </div>

      <div class="pay-methods">
        <button type="button" class="pay-method pay-method--cash" :disabled="!cart.items.length" @click="showCashPaymentNotice">
          <span>{{ t('cash') }}</span>
        </button>
        <button type="button" class="pay-method pay-method--card" :disabled="!cart.items.length" @click="orderFlow.step = 'customer'">
          <span class="card-main">{{ t('creditCard') }}</span>
          <span class="card-sub">{{ t('cardExtras') }}</span>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import { useStaffCall } from '../../../composables/useStaffCall'
import { productImage } from '../../../data/productImages'
import { useKioskI18n } from '../../../composables/useKioskI18n'

import logo from '../../../assets/kiosk/logo.png'
import bell from '../../../assets/kiosk/icons/bell.png'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import notifCircleRaw from '../../../assets/kiosk/icons/notif-circle.svg?raw'
import editPencilRaw from '../../../assets/kiosk/icons/edit-pencil.svg?raw'
import deleteXRaw from '../../../assets/kiosk/icons/delete-x.svg?raw'
import stepPillRaw from '../../../assets/kiosk/icons/step-pill.svg?raw'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
const { calling, justCalled, callStaff } = useStaffCall()
// 장바구니와 결제수단 영역도 상품 화면에서 선택한 언어를 그대로 사용합니다.
const { t, menuName, flavorName } = useKioskI18n()

const closeXSvg = closeXRaw
const notifCircleSvg = notifCircleRaw
const editPencilSvg = editPencilRaw
const deleteXSvg = deleteXRaw
const stepPillSvg = stepPillRaw

const CONTAINER_LABELS = { CUP: '컵', CONE: '콘', WAFFLE_CONE: '와플콘(+500원)' }

// 맛 선택도, 용기 선택도 필요 없는 상품(아이스크림 케이크/음료/커피 등)은 고칠 옵션 자체가 없다 -
// ProductStep.vue가 상품 선택 시 이런 상품을 곧장 장바구니에 담는 것과 같은 기준.
function canEditItem(item) {
  const product = orderFlow.products.find((p) => p.productId === item.productId)
  if (!product) return false
  return product.requiresFlavorSelection || orderFlow.needsContainerStep(product)
}

async function showCashPaymentNotice() {
  await orderFlow.showNotice(t('cashNotice'))
  orderFlow.finishOrder()
}

function itemDetail(item) {
  const parts = []
  if (item.sizeUpApplied) parts.push(t('sizeUpApplied'))
  if (item.containerType === 'CUP') parts.push(t('cup'))
  if (item.containerType === 'CONE') parts.push(t('cone'))
  if (item.containerType === 'WAFFLE_CONE') parts.push(t('waffleCone'))
  if (item.flavors.length) parts.push(item.flavors.map((f) => flavorName(f.flavorName)).join(', '))
  if (item.spoonCount) parts.push(`${t('spoon')} ${item.spoonCount}${t('piece')}`)
  if (item.dryIceMinutes) parts.push(`${t('dryIce')} ${item.dryIceMinutes}${t('minute')}`)
  return parts.join(' · ')
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  width: 100%;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  padding-bottom: 282px;
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

.close-btn :deep(svg) {
  width: 64px;
  height: 64px;
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

.step-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 116px;
  padding: 0 37px;
  background: #fafafa;
  flex-shrink: 0;
}

.step-pill {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 122px;
  height: 44px;
}

.step-pill :deep(svg) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.step-pill-text {
  position: relative;
  color: #fff;
  font-size: 18px;
  font-weight: 500;
}

.step-title {
  color: #ff53b8;
  font-size: 26px;
  font-weight: 500;
}

.cart-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.empty-cart {
  padding: 60px 20px;
  color: #999;
  font-size: 18px;
  text-align: center;
}

.cart-row {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 132px;
  padding: 0 40px;
}

.cart-thumb {
  width: 108px;
  height: 108px;
  object-fit: contain;
  flex-shrink: 0;
}

.cart-thumb--placeholder {
  background: #f4f4f4;
  border-radius: 8px;
}

.cart-item-text {
  flex: 1;
}

.cart-item-name {
  margin: 0;
  font-size: 26px;
  color: #000;
}

.cart-item-detail {
  margin: 6px 0 0;
  font-size: 18px;
  color: #999;
}

.cart-item-price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.price-original {
  color: #bbb;
  font-size: 16px;
  text-decoration: line-through;
}

.price-final {
  color: #000;
  font-size: 24px;
  font-weight: 500;
}

.cart-item-qty {
  min-width: 34px;
  text-align: center;
  font-size: 32px;
  color: #000;
}

.quantity-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.quantity-btn {
  width: 50px;
  height: 50px;
  border: 1px solid #f20c93;
  border-radius: 50%;
  background: #fff;
  color: #f20c93;
  font-size: 30px;
  line-height: 1;
  cursor: pointer;
}

.quantity-btn:disabled {
  border-color: #d9d9d9;
  color: #b9b9b9;
  cursor: not-allowed;
}

.row-icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.row-icon-btn :deep(svg) {
  width: 30px;
  height: 30px;
}

.add-more-btn {
  display: block;
  margin: 24px auto;
  width: 811px;
  max-width: calc(100% - 64px);
  height: 114px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #fff;
  color: #f20c93;
  font-size: 30px;
  cursor: pointer;
}

.pay-section {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  background: #f6f6f6;
  padding: 32px 40px;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.pay-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.pay-total {
  margin-left: auto;
  font-size: 38px;
  color: #f20c93;
}

.pay-methods {
  display: flex;
  gap: 24px;
}

.pay-method {
  flex: 1;
  height: 150px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 45px;
}

.pay-method:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pay-method--cash {
  border: 1px solid #f20c93;
  background: #fff;
  color: #f20c93;
}

.pay-method--card {
  border: none;
  background: #f20c93;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.card-sub {
  font-size: 22px;
  line-height: 1.4;
  text-align: left;
  white-space: pre-line;
}
</style>
