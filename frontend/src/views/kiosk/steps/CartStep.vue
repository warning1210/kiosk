<template>
  <!-- 6단계: 장바구니 확인 (CU-007) -->
  <div class="page">
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <div class="top-actions">
        <button type="button" class="icon-btn notif-btn" aria-label="알림">
          <span class="notif-circle" v-html="notifCircleSvg"></span>
          <img :src="bell" class="bell-icon" alt="" />
        </button>
        <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="orderFlow.goHome">
          <span v-html="closeXSvg"></span>
        </button>
      </div>
    </header>

    <div class="step-row">
      <span class="step-pill">
        <span v-html="stepPillSvg"></span>
        <span class="step-pill-text">STEP01</span>
      </span>
      <span class="step-title">주문 내역을 확인해주세요</span>
    </div>

    <ul class="cart-list">
      <li v-for="item in cart.items" :key="item.id" class="cart-row">
        <img v-if="item.imageUrl || productImage(item.productName)" :src="item.imageUrl || productImage(item.productName)" :alt="item.productName" class="cart-thumb" />
        <div v-else class="cart-thumb cart-thumb--placeholder" />
        <div class="cart-item-text">
          <p class="cart-item-name">{{ item.productName }}</p>
          <p class="cart-item-detail">{{ itemDetail(item) }}</p>
        </div>
        <div class="quantity-control" aria-label="수량 변경">
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
        <button type="button" class="row-icon-btn" aria-label="수정" @click="orderFlow.editItem(item)">
          <span v-html="editPencilSvg"></span>
        </button>
        <button type="button" class="row-icon-btn" aria-label="삭제" @click="orderFlow.removeFromCart(item.id)">
          <span v-html="deleteXSvg"></span>
        </button>
      </li>
    </ul>

    <button type="button" class="add-more-btn" @click="orderFlow.step = 'product'">메뉴 더 담으러 가기</button>

    <footer class="pay-section">
      <div class="pay-header">
        <span class="step-pill">
          <span v-html="stepPillSvg"></span>
          <span class="step-pill-text">STEP02</span>
        </span>
        <span class="step-title">결제 방법을 선택해주세요</span>
        <span class="pay-total">₩{{ cart.amountBeforeDiscount.toLocaleString() }}</span>
      </div>

      <div class="pay-methods">
        <button type="button" class="pay-method pay-method--cash" @click="showCashPaymentNotice">
          <span>현금</span>
        </button>
        <button type="button" class="pay-method pay-method--card" @click="orderFlow.step = 'customer'">
          <span class="card-main">신용카드</span>
          <span class="card-sub">모바일상품권<br />각종PAY<br />APP카드</span>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import { productImage } from '../../../data/productImages'

import logo from '../../../assets/kiosk/logo.png'
import bell from '../../../assets/kiosk/icons/bell.png'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import notifCircleRaw from '../../../assets/kiosk/icons/notif-circle.svg?raw'
import editPencilRaw from '../../../assets/kiosk/icons/edit-pencil.svg?raw'
import deleteXRaw from '../../../assets/kiosk/icons/delete-x.svg?raw'
import stepPillRaw from '../../../assets/kiosk/icons/step-pill.svg?raw'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()

const closeXSvg = closeXRaw
const notifCircleSvg = notifCircleRaw
const editPencilSvg = editPencilRaw
const deleteXSvg = deleteXRaw
const stepPillSvg = stepPillRaw

const CONTAINER_LABELS = { CUP: '컵', CONE: '콘', WAFFLE_CONE: '와플콘(+500원)' }

async function showCashPaymentNotice() {
  await orderFlow.showNotice('현금으로 결제하시면 카운터에서 결제를 도와드리겠습니다.')
  orderFlow.finishOrder()
}

function itemDetail(item) {
  const parts = []
  if (CONTAINER_LABELS[item.containerType]) parts.push(CONTAINER_LABELS[item.containerType])
  if (item.flavors.length) parts.push(item.flavors.map((f) => f.flavorName).join(', '))
  if (item.spoonCount) parts.push(`숟가락 ${item.spoonCount}개`)
  if (item.dryIceMinutes) parts.push(`드라이아이스 ${item.dryIceMinutes}분`)
  return parts.join(' · ')
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  padding-bottom: 282px;
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

.step-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100px;
  padding: 0 37px;
  background: #fafafa;
}

.step-pill {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 36px;
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
  font-size: 15px;
  font-weight: 500;
}

.step-title {
  color: #ff53b8;
  font-size: 20px;
  font-weight: 500;
}

.cart-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cart-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100px;
  padding: 0 40px;
}

.cart-thumb {
  width: 70px;
  height: 70px;
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
  font-size: 20px;
  color: #000;
}

.cart-item-detail {
  margin: 4px 0 0;
  font-size: 14px;
  color: #999;
}

.cart-item-qty {
  min-width: 28px;
  text-align: center;
  font-size: 25px;
  color: #000;
}

.quantity-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.quantity-btn {
  width: 38px;
  height: 38px;
  border: 1px solid #f20c93;
  border-radius: 50%;
  background: #fff;
  color: #f20c93;
  font-size: 24px;
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
  width: 24px;
  height: 24px;
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
  font-size: 30px;
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
  font-size: 18px;
  line-height: 1.4;
  text-align: left;
}
</style>
