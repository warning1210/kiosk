<template>
  <div class="checkout">
    <template v-if="loading">
      <p>주문 정보를 불러오는 중...</p>
    </template>
 
    <template v-else-if="errorMessage">
      <p class="error">{{ errorMessage }}</p>
    </template>
 
    <template v-else-if="checkoutInfo">
      <h2>{{ checkoutInfo.orderName }}</h2>
      <p class="amount">{{ formattedAmount }}원</p>
      <button @click="requestPayment" :disabled="paying">
        {{ paying ? '결제창 여는 중...' : '결제하기' }}
      </button>
    </template>
  </div>
</template>
 
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { loadTossPayments } from '@tosspayments/tosspayments-sdk'
import api from '../api'
 
const route = useRoute()
// 라우트가 /pay/:token 이라서 params에서 꺼냄 (기존 query.qrToken 방식에서 변경)
const qrToken = route.params.token
 
const loading = ref(true)
const paying = ref(false)
const errorMessage = ref('')
const checkoutInfo = ref(null)
 
const formattedAmount = computed(() =>
  checkoutInfo.value ? checkoutInfo.value.amount.toLocaleString() : ''
)
 
onMounted(async () => {
  if (!qrToken) {
    errorMessage.value = 'QR 정보가 없습니다.'
    loading.value = false
    return
  }
 
  try {
    // withCredentials: true 라서 쿠키가 있다면 이 요청에 자동으로 함께 전송됩니다.
    const { data } = await api.get(`/payments/${qrToken}/checkout`)
    checkoutInfo.value = data
  } catch (e) {
    errorMessage.value = 'QR이 만료되었거나 유효하지 않습니다.'
  } finally {
    loading.value = false
  }
})
 
async function requestPayment() {
  paying.value = true
  try {
    const tossPayments = await loadTossPayments(checkoutInfo.value.clientKey)
    const payment = tossPayments.payment({ customerKey: `kiosk_${qrToken}` })
 
    // 이 호출이 성공하면 브라우저가 토스페이먼츠 결제창(리다이렉트)으로 이동합니다.
    await payment.requestPayment({
      method: 'CARD',
      amount: {
        currency: 'KRW',
        value: checkoutInfo.value.amount,
      },
      orderId: checkoutInfo.value.orderId, // = qrToken
      orderName: checkoutInfo.value.orderName,
      successUrl: checkoutInfo.value.successUrl,
      failUrl: checkoutInfo.value.failUrl,
    })
  } catch (e) {
    paying.value = false
    errorMessage.value = '결제창을 여는 중 오류가 발생했습니다.'
  }
}
</script>
 
<style scoped>
.checkout {
  max-width: 420px;
  margin: 80px auto;
  text-align: center;
}
.amount {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 16px 0 32px;
}
button {
  width: 100%;
  padding: 14px;
  font-size: 1rem;
}
.error {
  color: #d33;
}
</style>