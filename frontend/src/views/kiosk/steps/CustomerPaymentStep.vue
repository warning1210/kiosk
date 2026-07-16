<template>
  <!-- 7단계: 포인트/할인 선택 (CU-008) -->
  <div>
    <h2>포인트 적립/사용</h2>
    <label>
      휴대폰 번호
      <input v-model="orderFlow.mobileNumberInput" type="text" placeholder="01012345678" />
    </label>
    <button type="button" @click="orderFlow.lookupCustomer">조회</button>

    <p v-if="orderFlow.isNewMember">
      신규 회원(FRIEND 등급)으로 시작합니다. 결제 완료 시 {{ orderFlow.estimatedEarnedPoints.toLocaleString() }}P가 적립됩니다.
    </p>
    <div v-if="orderFlow.customer">
      <p>{{ orderFlow.customer.grade }} 등급 · 보유 포인트 {{ orderFlow.customer.pointBalance.toLocaleString() }}P</p>
      <p>
        사용 포인트: {{ cart.usedPoints.toLocaleString() }}P
        (예상 적립 {{ orderFlow.estimatedEarnedPoints.toLocaleString() }}P)
      </p>
      <button type="button" @click="orderFlow.adjustUsedPoints(-1000)">-1000</button>
      <button type="button" @click="orderFlow.adjustUsedPoints(-100)">-100</button>
      <button type="button" @click="orderFlow.adjustUsedPoints(100)">+100</button>
      <button type="button" @click="orderFlow.adjustUsedPoints(1000)">+1000</button>
      <button type="button" @click="orderFlow.useMaxPoints">최대금액사용</button>
    </div>

    <p>결제 금액: {{ cart.totalAmount.toLocaleString() }}원</p>

    <button type="button" @click="orderFlow.step = 'cart'">뒤로</button>

    <!-- CU-009: 결제 실행 -->
    <div v-if="!orderFlow.qrInfo">
      <button type="button" :disabled="orderFlow.checkoutInProgress" @click="orderFlow.startPayment">결제</button>
      <p v-if="orderFlow.checkoutError">{{ orderFlow.checkoutError }}</p>
    </div>
    <div v-else class="modal-backdrop">
      <div class="modal">
        <button type="button" class="modal-close" @click="orderFlow.closeQrModal">×</button>
        <p>결제 금액: {{ orderFlow.qrInfo.requestedAmount.toLocaleString() }}원</p>
        <img :src="orderFlow.qrDataUrl" alt="결제 QR코드" width="200" height="200" />
        <p>휴대폰으로 QR코드를 스캔해서 결제를 완료해주세요.</p>
        <button type="button" class="test-open-pay" @click="openPayPageForTest">
          (임시 테스트용) 결제 페이지 새 탭에서 열기
        </button>
        <p>상태: {{ orderFlow.paymentStatusLabel }}</p>
        <p v-if="orderFlow.paymentStatus === 'PAID'">결제가 완료되었습니다. 감사합니다!</p>
        <!-- CU-009-2: 포인트를 적립하지 않은 사용자에게 한 번 더 안내 -->
        <p v-if="orderFlow.paymentStatus === 'PAID' && !cart.customerMobileNumber">
          포인트를 적립하지 않으셨습니다. 다음 방문 시 휴대폰 번호를 입력하시면 적립 혜택을 받으실 수 있습니다.
        </p>
        <!-- CU-009-1: 결제 실패 시 QR코드 재생성 -->
        <button v-else type="button" @click="orderFlow.regenerateQr">QR코드 재생성</button>

        <h3>결제 요청 JSON</h3>
        <pre class="payment-json">{{ orderFlow.checkoutJsonText }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()

// 임시: QR을 휴대폰으로 스캔하기 어려운 개발 환경(localhost)에서 결제 페이지를 바로 테스트하기 위한 버튼
function openPayPageForTest() {
  window.open(`/pay/${orderFlow.qrInfo.qrToken}`, '_blank')
}
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
  position: relative;
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
}

.modal-close {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}

.test-open-pay {
  display: block;
  margin: 0.5rem auto;
  padding: 0.5rem 0.75rem;
  color: #b8860b;
  border: 1px dashed #b8860b;
  background: #fff8e1;
  font-size: 0.8rem;
}

.payment-json {
  max-width: 320px;
  max-height: 200px;
  overflow: auto;
  background: #f4f4f4;
  padding: 0.75rem;
  text-align: left;
  font-size: 0.8rem;
}
</style>
