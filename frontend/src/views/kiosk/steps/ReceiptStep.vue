<template>
  <!-- 결제 완료 후 영수증 화면 -->
  <div class="page">
    <img class="logo" :src="logo" alt="배스킨라빈스" />

    <div class="done-badge">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="m8.5 12 2.5 2.5 4.5-5"/></svg>
    </div>
    <h2 class="done-title">{{ isCashOrder ? '주문서를 카운터에 제출해주세요' : '결제가 완료되었습니다' }}</h2>

    <!-- 실제 종이 영수증과 비슷하게 보이도록 만든 영역.
         '영수증 다시 출력'은 이 화면과 별개로 강사님 프린터로 출력을 다시 요청한다. -->
    <div v-if="r" class="receipt-paper" id="receipt-paper">
      <p class="store">{{ r.storeName }}</p>
      <p class="order-no">
        {{ isCashOrder ? '주문번호(현금주문)' : '주문번호' }}
        {{ isCashOrder ? r.waitingNumber : r.orderNumber }}
      </p>
      <p class="type">{{ r.orderType === 'DINE_IN' ? '매장 식사' : '포장' }}</p>
      <hr />

      <div v-for="(item, i) in r.items" :key="i" class="line-item">
        <div class="row">
          <span>{{ item.productName }}</span>
          <span>{{ item.lineTotal.toLocaleString() }}원</span>
        </div>
        <!-- 옵션(용기/스푼/드라이아이스)과 맛을 상품 아래에 작게 표시 -->
        <p v-if="item.containerType !== 'NONE'" class="option">
          용기: {{ item.containerType === 'CUP' ? '컵' : item.containerType === 'WAFFLE_CONE' ? '와플콘' : '콘' }}
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
      <div class="row total"><span>{{ isCashOrder ? '카운터 결제 예정 금액' : '결제 금액' }}</span><span>{{ r.finalAmount.toLocaleString() }}원</span></div>
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
      <button type="button" class="print-btn" :disabled="orderFlow.printing" @click="orderFlow.printReceipt">
        {{ orderFlow.printing ? '출력 중...' : '영수증 다시 출력' }}
      </button>
      <button type="button" class="confirm-btn" @click="orderFlow.finishOrder">완료 (처음으로)</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import logo from '../../../assets/kiosk/logo.png'

const orderFlow = useOrderFlowStore()
const r = computed(() => orderFlow.receipt)
const isCashOrder = computed(() => r.value?.paymentMethod === 'CASH')

const PAYMENT_METHOD_LABELS = { QR: 'QR 간편결제', CARD: '신용카드', CASH: '현금', POINT: '포인트', EASY_PAY: '간편결제' }
const paymentMethodLabel = computed(() => PAYMENT_METHOD_LABELS[r.value?.paymentMethod] ?? r.value?.paymentMethod ?? '-')

function formatDate(value) {
  return new Date(value).toLocaleString('ko-KR')
}
</script>

<style scoped>
.page {
  max-width: 1024px;
  width: 100%;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 0;
  padding: 48px 40px 60px;
  gap: 20px;
}

.logo {
  width: 112px;
  height: 100px;
  object-fit: contain;
  margin-bottom: 4px;
}

.done-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #fff0f8;
  color: #f20c93;
}

.done-badge svg {
  width: 52px;
  height: 52px;
}

.done-title {
  margin: 0;
  font-size: 44px;
  font-weight: 500;
  color: #000;
}

/* 실제 종이 영수증 느낌을 유지하되, 키오스크 화면 안에서 읽기 편하도록 큼직하게 */
.receipt-paper {
  width: 100%;
  max-width: 580px;
  background: #fff;
  border: 1px solid #e2e2e2;
  border-radius: 20px;
  padding: 44px 48px;
  font-size: 24px;
  text-align: center;
  box-shadow: 0 10px 28px rgb(0 0 0 / 8%);
}

.receipt-paper .store {
  font-weight: 700;
  font-size: 32px;
  margin: 0 0 10px;
}

.receipt-paper .order-no {
  margin: 8px 0;
}

.receipt-paper .type {
  margin: 8px 0;
  color: #666;
}

.receipt-paper hr {
  border: none;
  border-top: 2px dashed #e2e2e2;
  margin: 22px 0;
}

.line-item {
  text-align: left;
  margin-bottom: 18px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.row.total {
  font-weight: 700;
  font-size: 30px;
  color: #f20c93;
}

.option {
  margin: 6px 0 0;
  font-size: 20px;
  color: #999;
}

.pay-info {
  margin: 8px 0;
  font-size: 21px;
  color: #666;
}

.thanks {
  margin-top: 24px;
  font-size: 22px;
  color: #333;
}

.loading {
  font-size: 26px;
  color: #999;
}

.print-message {
  color: #666;
  font-size: 24px;
}

.actions {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.print-btn {
  height: 92px;
  padding: 0 40px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #fff;
  color: #f20c93;
  font-size: 28px;
  cursor: pointer;
}

.print-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.confirm-btn {
  height: 92px;
  padding: 0 48px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #f20c93;
  color: #fff;
  font-size: 30px;
  cursor: pointer;
}
</style>
