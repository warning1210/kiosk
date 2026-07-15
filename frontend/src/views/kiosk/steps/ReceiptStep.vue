<template>
  <!-- 결제 완료 후 영수증 화면 -->
  <div class="receipt-step">
    <h2 class="done-title">✔ 결제가 완료되었습니다</h2>

    <!-- 실제 종이 영수증과 비슷하게 보이도록 만든 영역.
         '영수증 다시 출력'은 이 화면과 별개로 강사님 프린터로 출력을 다시 요청한다. -->
    <div v-if="r" class="receipt-paper" id="receipt-paper">
      <p class="store">{{ r.storeName }}</p>
      <p class="order-no">주문번호 {{ r.orderNumber }}</p>
      <p class="type">{{ r.orderType === 'DINE_IN' ? '매장 식사' : '포장' }}</p>
      <hr />

      <div v-for="(item, i) in r.items" :key="i" class="line-item">
        <div class="row">
          <span>{{ item.productName }}</span>
          <span>{{ item.lineTotal.toLocaleString() }}원</span>
        </div>
        <!-- 옵션(용기/스푼/드라이아이스)과 맛을 상품 아래에 작게 표시 -->
        <p v-if="item.containerType !== 'NONE'" class="option">
          용기: {{ item.containerType === 'CUP' ? '컵' : '콘' }}
        </p>
        <p v-if="item.flavors.length" class="option">
          맛: {{ item.flavors.map((f) => f.quantity > 1 ? `${f.flavorName} x${f.quantity}` : f.flavorName).join(', ') }}
        </p>
      </div>

      <hr />
      <div class="row"><span>주문 금액</span><span>{{ r.amountBeforeDiscount.toLocaleString() }}원</span></div>
      <div v-if="r.discountAmount > 0" class="row">
        <span>포인트 사용</span><span>-{{ r.discountAmount.toLocaleString() }}원</span>
      </div>
      <div class="row total"><span>결제 금액</span><span>{{ r.finalAmount.toLocaleString() }}원</span></div>
      <div v-if="r.earnedPoints > 0" class="row"><span>적립 포인트</span><span>{{ r.earnedPoints.toLocaleString() }}P</span></div>

      <hr />
      <p class="pay-info">결제수단: {{ paymentMethodLabel }}</p>
      <p v-if="r.approvalNumber" class="pay-info">승인번호: {{ r.approvalNumber }}</p>
      <p v-if="r.paidAt" class="pay-info">{{ formatDate(r.paidAt) }}</p>
      <p class="thanks">이용해 주셔서 감사합니다</p>
    </div>

    <p v-else class="loading">영수증을 불러오는 중...</p>

    <!-- 프린터 출력 결과 안내 -->
    <p v-if="orderFlow.printMessage" class="print-message">{{ orderFlow.printMessage }}</p>

    <div class="actions">
      <button type="button" :disabled="orderFlow.printing" @click="orderFlow.printReceipt">
        {{ orderFlow.printing ? '출력 중...' : '영수증 다시 출력' }}
      </button>
      <button type="button" class="primary" @click="orderFlow.finishOrder">완료 (처음으로)</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'

const orderFlow = useOrderFlowStore()
const r = computed(() => orderFlow.receipt)

const PAYMENT_METHOD_LABELS = { QR: 'QR 간편결제', CARD: '신용카드', CASH: '현금', POINT: '포인트', EASY_PAY: '간편결제' }
const paymentMethodLabel = computed(() => PAYMENT_METHOD_LABELS[r.value?.paymentMethod] ?? r.value?.paymentMethod ?? '-')

function formatDate(value) {
  return new Date(value).toLocaleString('ko-KR')
}
</script>

<style scoped>
.receipt-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
}

.done-title {
  color: #2e8c77;
}

/* 감열 영수증처럼 좁고 흰 종이 느낌 */
.receipt-paper {
  width: 280px;
  background: #fff;
  border: 1px solid #ddd;
  padding: 1rem 1.25rem;
  font-size: 0.85rem;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.receipt-paper .store {
  font-weight: 700;
  font-size: 1rem;
  margin: 0 0 0.25rem;
}

.receipt-paper .order-no {
  margin: 0.25rem 0;
}

.receipt-paper hr {
  border: none;
  border-top: 1px dashed #bbb;
  margin: 0.5rem 0;
}

.line-item {
  text-align: left;
  margin-bottom: 0.5rem;
}

.row {
  display: flex;
  justify-content: space-between;
}

.row.total {
  font-weight: 700;
  font-size: 0.95rem;
}

.option {
  margin: 0.1rem 0 0;
  font-size: 0.75rem;
  color: #666;
}

.pay-info {
  margin: 0.15rem 0;
  font-size: 0.78rem;
  color: #444;
}

.thanks {
  margin-top: 0.75rem;
}

.print-message {
  color: #555;
  font-size: 0.9rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
}

.actions .primary {
  background: #2e8c77;
  color: #fff;
  border: none;
  padding: 0.6rem 1.5rem;
  border-radius: 6px;
  cursor: pointer;
}
</style>
