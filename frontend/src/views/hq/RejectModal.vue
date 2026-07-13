<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>반려 사유</h3>
      <textarea v-model="reason" rows="3" placeholder="반려 사유를 입력해주세요"></textarea>
      <p v-if="error" class="error">{{ error }}</p>
      <footer>
        <button type="button" class="cancel" @click="$emit('close')">취소</button>
        <button type="button" class="submit" :disabled="submitting" @click="submit">반려 처리</button>
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
const reason = ref('')
const error = ref('')

function submit() {
  if (!reason.value.trim()) {
    error.value = '반려 사유를 입력해주세요'
    return
  }
  emit('confirm', reason.value.trim())
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

textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
  box-sizing: border-box;
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
  background: #dc2626;
  color: white;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
}
</style>
