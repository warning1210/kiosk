<template>
  <div class="success">
    <p v-if="status === 'confirming'">결제를 확인하고 있어요...</p>

    <template v-else-if="status === 'done'">
      <h2>결제가 완료되었습니다</h2>
      <dl class="summary">
        <dt>주문번호</dt>
        <dd>#{{ result.orderId }}</dd>

        <dt>결제 금액</dt>
        <dd>{{ result.paidAmount.toLocaleString() }}원</dd>

        <dt>승인번호</dt>
        <dd>{{ result.approvalNumber }}</dd>

        <dt>결제 시각</dt>
        <dd>{{ formattedPaidAt }}</dd>
      </dl>
      <p class="hint">키오스크 화면을 확인해주세요.</p>
      <button type="button" class="close-btn" @click="closeWindow">닫기</button>
    </template>

    <p v-else class="error">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'

const route = useRoute()
const status = ref('confirming') // 'confirming' | 'done' | 'failed'
const errorMessage = ref('')
const result = ref(null)

const formattedPaidAt = computed(() => {
  if (!result.value?.paidAt) return ''
  return new Date(result.value.paidAt).toLocaleString('ko-KR')
})

onMounted(async () => {
  const { qrToken, paymentKey, orderId, amount } = route.query

  try {
    const { data } = await http.post('/payments/toss/confirm', {
      qrToken,
      paymentKey,
      orderId,
      amount: Number(amount),
    })

    result.value = data
    status.value = 'done'
  } catch (e) {
    status.value = 'failed'
    errorMessage.value = e.response?.data?.message || '결제 승인에 실패했습니다.'
  }
})

// 새 창(팝업)으로 열렸을 때만 스스로 닫을 수 있으므로, 실패하면 그냥 둔다(사용자가 직접 닫음)
function closeWindow() {
  window.close()
}
</script>

<style scoped>
.success {
  max-width: 420px;
  margin: 80px auto;
  text-align: center;
}
.summary {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 16px;
  text-align: left;
  margin: 24px 0;
}
.summary dt {
  color: #888;
}
.summary dd {
  font-weight: 600;
}
.hint {
  color: #888;
  font-size: 0.9rem;
}
.close-btn {
  margin-top: 24px;
  padding: 12px 32px;
  border: 1px solid #b9b9b9;
  border-radius: 40px;
  background: #fff;
  cursor: pointer;
}
.error {
  color: #d33;
}
</style>
