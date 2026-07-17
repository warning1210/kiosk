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
import { onMounted, onUnmounted } from 'vue'
import { useOrderFlowStore } from '../../stores/orderFlow'
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'
import ReceiptStep from './steps/ReceiptStep.vue'

const orderFlow = useOrderFlowStore()

onMounted(() => {
  orderFlow.init()
})

onUnmounted(() => {
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
