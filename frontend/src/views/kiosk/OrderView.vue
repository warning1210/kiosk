<template>
  <section class="order" :class="{ 'easy-mode': orderFlow.easyMode }">
    <div v-if="isBusy" class="busy-banner">
      현재 매장이 혼잡하여 주문이 지연될 수 있습니다 (약 {{ estimatedWaitMinutes }}분)
    </div>

    <OrderTypeStep v-if="orderFlow.step === 'orderType'" />
    <ProductStep v-else-if="orderFlow.step === 'product'" />
    <ContainerStep v-else-if="orderFlow.step === 'container'" />
    <FlavorStep v-else-if="orderFlow.step === 'flavor'" />
    <CartStep v-else-if="orderFlow.step === 'cart'" />
    <CustomerPaymentStep v-else-if="orderFlow.step === 'customer'" />
    <ReceiptStep v-else-if="orderFlow.step === 'receipt'" />

    <div v-if="showIdleWarning" class="modal-backdrop idle-backdrop">
      <div class="modal idle-modal" role="dialog" aria-modal="true" aria-labelledby="idle-title">
        <p id="idle-title" class="idle-title">{{ t('extendOrderTitle') }}</p>
        <p class="idle-message">{{ t('returnToAds').replace('{seconds}', idleCountdown) }}</p>
        <div class="confirm-actions">
          <button type="button" class="confirm-cancel" @click="returnToAdvertisement">{{ t('no') }}</button>
          <button type="button" class="confirm-ok" @click="extendOrderTime">{{ t('extend') }}</button>
        </div>
      </div>
    </div>

    <!-- 어느 화면에서든 뜰 수 있는 삭제/이탈 확인 팝업 - 브라우저 기본 confirm() 대신 앱 디자인으로 통일 -->
    <div v-if="orderFlow.confirmDialog" class="modal-backdrop">
      <div class="modal">
        <p class="confirm-message">{{ orderFlow.confirmDialog.message }}</p>
        <div class="confirm-actions">
          <button v-if="!orderFlow.confirmDialog.noticeOnly" type="button" class="confirm-cancel" @click="orderFlow.resolveConfirm(false)">{{ orderFlow.confirmDialog.cancelLabel ?? '취소' }}</button>
          <button type="button" class="confirm-ok" @click="orderFlow.resolveConfirm(true)">{{ orderFlow.confirmDialog.confirmLabel ?? '확인' }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderFlowStore } from '../../stores/orderFlow'
import { useCartStore } from '../../stores/cart'
import { useBranchBusyBanner } from '../../composables/useBranchBusyBanner'
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'
import ReceiptStep from './steps/ReceiptStep.vue'
import { useKioskI18n } from '../../composables/useKioskI18n'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
const router = useRouter()
const { isBusy, estimatedWaitMinutes } = useBranchBusyBanner()
// 주문 연장 팝업도 상품 화면에서 선택한 언어를 그대로 사용합니다.
const { t } = useKioskI18n()

const IDLE_TIMEOUT_MS = 60_000
const IDLE_WARNING_SECONDS = 10
const showIdleWarning = ref(false)
const idleCountdown = ref(IDLE_WARNING_SECONDS)
let idleTimer = null
let countdownTimer = null

function clearIdleTimers() {
  window.clearTimeout(idleTimer)
  window.clearInterval(countdownTimer)
  idleTimer = null
  countdownTimer = null
}

function startIdleTimer() {
  window.clearTimeout(idleTimer)
  idleTimer = window.setTimeout(showIdlePrompt, IDLE_TIMEOUT_MS)
}

function resetIdleTimer() {
  if (showIdleWarning.value) return
  startIdleTimer()
}

function showIdlePrompt() {
  showIdleWarning.value = true
  idleCountdown.value = IDLE_WARNING_SECONDS
  window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    idleCountdown.value -= 1
    if (idleCountdown.value <= 0) returnToAdvertisement()
  }, 1000)
}

function extendOrderTime() {
  showIdleWarning.value = false
  window.clearInterval(countdownTimer)
  countdownTimer = null
  startIdleTimer()
}

function returnToAdvertisement() {
  if (!showIdleWarning.value) return
  showIdleWarning.value = false
  clearIdleTimers()
  orderFlow.stopPolling()
  if (orderFlow.confirmDialog) orderFlow.resolveConfirm(false)
  orderFlow.closeQrModal()
  cart.clear()
  router.push('/')
}

onMounted(() => {
  orderFlow.init()
  window.addEventListener('pointerdown', resetIdleTimer, { passive: true })
  window.addEventListener('keydown', resetIdleTimer)
  startIdleTimer()
})

onUnmounted(() => {
  clearIdleTimers()
  window.removeEventListener('pointerdown', resetIdleTimer)
  window.removeEventListener('keydown', resetIdleTimer)
  orderFlow.stopPolling()
})
</script>

<style scoped>
.busy-banner {
  padding: 14px 20px;
  margin-bottom: 12px;
  color: #fff;
  background: rgb(220 30 30 / 92%);
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  text-align: center;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 30;
}

.modal {
  width: min(480px, 90vw);
  background: #fff;
  border-radius: 26px;
  padding: 40px 32px;
  text-align: center;
}

.idle-backdrop {
  z-index: 50;
}

.idle-modal {
  width: min(560px, 90vw);
}

.idle-title {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 700;
  color: #000;
}

.idle-message {
  margin: 0 0 32px;
  font-size: 20px;
  color: #555;
}

.confirm-message {
  margin: 0 0 32px;
  font-size: 20px;
  color: #000;
  line-height: 1.4;
  white-space: pre-line;
}

.confirm-actions {
  display: flex;
  gap: 16px;
}

.confirm-cancel,
.confirm-ok {
  flex: 1;
  height: 64px;
  border-radius: 99px;
  font-size: 18px;
  cursor: pointer;
}

.confirm-cancel {
  border: 1px solid #b9b9b9;
  background: #fff;
  color: #f20c93;
}

.confirm-ok {
  border: none;
  background: #f20c93;
  color: #fff;
}

/* 쉬운모드는 OrderView에 한 번만 클래스를 붙이고, 각 주문 단계의 공통 버튼·문구를 함께 확대한다.
   개별 목록의 열 수와 페이지당 개수는 ProductStep/FlavorStep에서 별도로 2열 기준으로 맞춘다. */
.order.easy-mode :deep(.tab) {
  min-height: 112px;
  font-size: 36px;
  font-weight: 700;
}

.order.easy-mode :deep(.category-tab) {
  min-height: 70px;
  padding: 16px 34px;
  font-size: 34px;
  font-weight: 700;
}

.order.easy-mode :deep(.prev-btn),
.order.easy-mode :deep(.confirm-btn),
.order.easy-mode :deep(.checkout-btn),
.order.easy-mode :deep(.add-more-btn),
.order.easy-mode :deep(.pay-method) {
  min-height: 118px;
  font-size: 34px;
  font-weight: 800;
}

.order.easy-mode :deep(.progress-text),
.order.easy-mode :deep(.step-title),
.order.easy-mode :deep(.section-title),
.order.easy-mode :deep(.product-desc),
.order.easy-mode :deep(.warning-text) {
  font-size: 32px;
  line-height: 1.45;
}

.order.easy-mode :deep(.cart-item-name),
.order.easy-mode :deep(.cart-item-detail),
.order.easy-mode :deep(.price-final),
.order.easy-mode :deep(.option-label) {
  font-size: 30px;
  line-height: 1.4;
}

.order.easy-mode :deep(.quantity-btn),
.order.easy-mode :deep(.row-icon-btn),
.order.easy-mode :deep(.icon-btn) {
  min-width: 72px;
  min-height: 72px;
}

/* 쉬운모드의 주문 내역은 한 행의 높이도 함께 늘려, 커진 글자와 수량 버튼이 서로 겹치지 않게 한다. */
.order.easy-mode :deep(.cart-row) {
  min-height: 190px;
  height: auto;
  gap: 24px;
  padding: 20px 34px;
}

.order.easy-mode :deep(.cart-thumb) {
  width: 145px;
  height: 145px;
}

.order.easy-mode :deep(.cart-item-name) {
  font-size: 34px;
  font-weight: 700;
}

.order.easy-mode :deep(.cart-item-detail) {
  font-size: 25px;
  color: #666;
}

.order.easy-mode :deep(.price-original) {
  font-size: 22px;
}

.order.easy-mode :deep(.price-final),
.order.easy-mode :deep(.cart-item-qty) {
  font-size: 34px;
  font-weight: 700;
}

.order.easy-mode :deep(.quantity-btn) {
  width: 72px;
  height: 72px;
  font-size: 40px;
  border-width: 2px;
}

.order.easy-mode :deep(.row-icon-btn svg) {
  width: 42px;
  height: 42px;
}

/* 결제 영역은 금액과 선택 버튼을 조금 더 키워 멀리서도 현재 결제 금액과 다음 행동을 구분할 수 있게 한다. */
.order.easy-mode :deep(.pay-total) {
  font-size: 48px;
  font-weight: 700;
}

.order.easy-mode :deep(.pay-method) {
  min-height: 175px;
  font-size: 50px;
}

.order.easy-mode :deep(.card-sub),
.order.easy-mode :deep(.coupon-hint),
.order.easy-mode :deep(.customer-hint),
.order.easy-mode :deep(.point-desc),
.order.easy-mode :deep(.modal-subtitle),
.order.easy-mode :deep(.status-line),
.order.easy-mode :deep(.timer-note) {
  font-size: 30px;
  line-height: 1.5;
}

.order.easy-mode :deep(.point-label),
.order.easy-mode :deep(.lookup-btn),
.order.easy-mode :deep(.coupon-use-btn) {
  font-size: 34px;
  font-weight: 700;
}

.order.easy-mode :deep(.point-card) {
  width: 270px;
  height: 270px;
  border-width: 4px;
}

.order.easy-mode :deep(.summary-final) {
  height: 120px;
  font-size: 52px;
  font-weight: 700;
}

.order.easy-mode :deep(.summary-breakdown) {
  height: 115px;
  font-size: 34px;
}

/* 직원 호출 알림은 ProductStep과 CartStep에 각각 렌더링되므로 부모 OrderView에서 공통으로 확대한다.
   쉬운모드일 때 글자뿐 아니라 패딩과 최소 폭도 같이 늘려 짧은 토스트가 눈에 잘 띄게 한다. */
.order.easy-mode :deep(.staff-call-toast) {
  min-width: 620px;
  max-width: 88vw;
  padding: 24px 34px;
  border-radius: 999px;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.4;
  text-align: center;
  box-sizing: border-box;
}

/* 60초 동안 입력이 없을 때 나타나는 경고도 쉬운모드 상태를 그대로 따라간다.
   제목·남은 초·선택 버튼을 모두 확대해 자동 복귀 전에 사용자가 쉽게 연장할 수 있게 한다. */
.order.easy-mode .idle-modal {
  width: min(720px, 94vw);
  padding: 54px 44px;
  border-radius: 34px;
}

.order.easy-mode .idle-title {
  margin-bottom: 24px;
  font-size: 42px;
  line-height: 1.35;
}

.order.easy-mode .idle-message {
  margin-bottom: 42px;
  font-size: 34px;
  line-height: 1.5;
}

.order.easy-mode .confirm-cancel,
.order.easy-mode .confirm-ok {
  height: 92px;
  font-size: 30px;
  font-weight: 800;
}

.order.easy-mode .confirm-message {
  font-size: 32px;
  line-height: 1.55;
}
</style>
