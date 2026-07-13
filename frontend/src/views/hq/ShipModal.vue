<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>배송 등록</h3>
      <label class="field">
        <span>운송장번호</span>
        <input v-model="trackingNumber" type="text" placeholder="운송장번호" />
      </label>
      <label class="field">
        <span>택배사</span>
        <input v-model="courierName" type="text" placeholder="예: CJ대한통운" />
      </label>
      <label class="field">
        <span>배송담당자</span>
        <input v-model="driverName" type="text" placeholder="담당자명" />
      </label>
      <label class="field">
        <span>예상 도착일시</span>
        <input v-model="estimatedArrivalAt" type="datetime-local" />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <footer>
        <button type="button" class="cancel" @click="$emit('close')">취소</button>
        <button type="button" class="submit" :disabled="submitting" @click="submit">배송 등록</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'confirm'])
const trackingNumber = ref('')
const courierName = ref('')
const driverName = ref('')
const estimatedArrivalAt = ref('')
const error = ref('')

function submit() {
  if (!trackingNumber.value.trim()) {
    error.value = '운송장번호를 입력해주세요'
    return
  }
  emit('confirm', {
    trackingNumber: trackingNumber.value.trim(),
    courierName: courierName.value.trim() || null,
    driverName: driverName.value.trim() || null,
    estimatedArrivalAt: estimatedArrivalAt.value || null
  })
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 15, 25, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.modal {
  width: 360px;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
}

h3 {
  margin: 0 0 0.75rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: #374151;
  margin-bottom: 0.625rem;
}

.field input {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
}

.error {
  color: #dc2626;
  font-size: 0.8125rem;
  margin: 0.375rem 0 0;
}

footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.cancel {
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  cursor: pointer;
}

.submit {
  border: none;
  background: #4f46e5;
  color: white;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
}
</style>
