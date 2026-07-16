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
      <p>결제창 여는 중...</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { loadTossPayments } from '@tosspayments/tosspayments-sdk'
import http from '../../api/http'

// 팝업으로 띄울 때는 prop으로 토큰을 받고, 고객이 직접 QR을 스캔해 /pay/:token으로 들어온
// 경우엔 prop이 없으니 라우트 파라미터를 그대로 쓴다.
const props = defineProps({ qrToken: { type: String, default: '' } })
const route = useRoute()
const qrToken = props.qrToken || route.params.token

const loading = ref(true)
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
    const { data } = await http.get(`/payments/${qrToken}/checkout`)
    checkoutInfo.value = data
  } catch (e) {
    errorMessage.value = 'QR이 만료되었거나 유효하지 않습니다.'
    loading.value = false
    return
  }
  loading.value = false
  // 결제하기 버튼 없이, 정보를 불러오는 즉시 바로 결제창으로 넘어간다
  requestPayment()
})

async function requestPayment() {
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
    errorMessage.value = '결제창을 여는 중 오류가 발생했습니다.'
  }
}
</script>

<style scoped>
.checkout {
  max-width: 420px;
  margin: 0 auto;
  text-align: center;
}
.amount {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 16px 0 32px;
}
.error {
  color: #d33;
}
</style>
