<template>
  <section class="order">
    <p>단계: {{ orderFlow.stepLabel }}</p>

    <OrderTypeStep v-if="orderFlow.step === 'orderType'" />
    <ProductStep v-else-if="orderFlow.step === 'product'" />
    <ContainerStep v-else-if="orderFlow.step === 'container'" />
    <FlavorStep v-else-if="orderFlow.step === 'flavor'" />
    <CartStep v-else-if="orderFlow.step === 'cart'" />
    <CustomerPaymentStep v-else-if="orderFlow.step === 'customer'" />
    <ReceiptStep v-else-if="orderFlow.step === 'receipt'" />

    <div v-if="showIdleWarning" class="modal-backdrop idle-backdrop">
      <div class="modal idle-modal" role="dialog" aria-modal="true" aria-labelledby="idle-title">
        <p id="idle-title" class="idle-title">주문 시간을 연장하시겠습니까?</p>
        <p class="idle-message">{{ idleCountdown }}초 후 광고 화면으로 돌아갑니다.</p>
        <div class="confirm-actions">
          <button type="button" class="confirm-cancel" @click="returnToAdvertisement">아니요</button>
          <button type="button" class="confirm-ok" @click="extendOrderTime">연장하기</button>
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
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'
import ReceiptStep from './steps/ReceiptStep.vue'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
const router = useRouter()

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
</style>
