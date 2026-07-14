<template>
  <div class="success">
    <p v-if="status === 'confirming'">결제를 확인하고 있어요...</p>
    <p v-else-if="status === 'done'">결제가 완료되었습니다! 키오스크 화면을 확인해주세요.</p>
    <p v-else class="error">{{ errorMessage }}</p>
  </div>
</template>
 
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
 
const route = useRoute()
const status = ref('confirming') // 'confirming' | 'done' | 'failed'
const errorMessage = ref('')
 
onMounted(async () => {
  // 토스가 successUrl로 리다이렉트하면서 붙여준 값들
  const { qrToken, paymentKey, orderId, amount } = route.query
 
  try {
    // withCredentials: true 라서
    // - 요청 시 브라우저에 있는 쿠키(예: 로그인/세션 쿠키)가 자동으로 함께 전송되고
    // - 응답에 Set-Cookie가 있다면 브라우저가 자동으로 저장합니다.
    // 즉 쿠키를 직접 꺼내서 실어보내거나, 응답에서 다시 꺼내 저장할 필요가 없습니다.
    await api.post('/payments/toss/confirm', {
      qrToken,
      paymentKey,
      orderId,
      amount: Number(amount),
    })
 
    status.value = 'done'
  } catch (e) {
    status.value = 'failed'
    errorMessage.value = e.response?.data?.message || '결제 승인에 실패했습니다.'
  }
})
</script>
 
<style scoped>
.success {
  max-width: 420px;
  margin: 80px auto;
  text-align: center;
}
.error {
  color: #d33;
}
</style>