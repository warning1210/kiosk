<template>
  <section class="pay">
    <h2>결제 확인</h2>
    <p v-if="loading">불러오는 중...</p>
    <template v-else>
      <p>결제 금액: {{ status?.requestedAmount?.toLocaleString() }}원</p>
      <p>상태: {{ status?.paymentStatus }}</p>
      <button v-if="status?.paymentStatus === 'QR_CREATED'" type="button" @click="confirmPayment">
        결제 완료 처리
      </button>
      <p v-else-if="status?.paymentStatus === 'PAID'">결제가 완료되었습니다.</p>
      <p v-if="error">{{ error }}</p>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'

const route = useRoute()
const status = ref(null)
const loading = ref(true)
const error = ref('')

async function loadStatus() {
  const { data } = await http.get(`/payments/${route.params.token}`)
  status.value = data
}

async function confirmPayment() {
  try {
    const { data } = await http.post(`/payments/${route.params.token}/confirm`)
    status.value = data
  } catch (e) {
    error.value = e.response?.data?.message ?? '결제 확인에 실패했습니다.'
  }
}

onMounted(async () => {
  try {
    await loadStatus()
  } catch (e) {
    error.value = '결제 정보를 찾을 수 없습니다.'
  } finally {
    loading.value = false
  }
})
</script>
