<template>
  <!-- 1단계: 매장/포장 선택 (CU-002) - 팝업 형태 -->
  <div class="modal-backdrop">
    <div class="modal">
      <h2>매장에서 드시나요, 포장하시나요?</h2>
      <p class="subtitle">Dine in or Takeout?</p>
      <button type="button" @click="orderFlow.selectOrderType('DINE_IN')">매장 (Dine in)</button>
      <button type="button" @click="orderFlow.selectOrderType('TAKEOUT')">포장 (Takeout)</button>
    </div>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'

const orderFlow = useOrderFlowStore()
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
}

.subtitle {
  color: #666;
}
</style>
