<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <header>
        <h3>재고 신청</h3>
        <button type="button" class="close" @click="$emit('close')">×</button>
      </header>

      <div class="body">
        <div v-for="(row, index) in rows" :key="index" class="row">
          <select v-model.number="row.flavorId">
            <option :value="null" disabled>맛 선택</option>
            <option v-for="item in inventoryItems" :key="item.flavorId" :value="item.flavorId">
              {{ item.flavorName }} (현재고 {{ item.currentQuantity }})
            </option>
          </select>
          <input v-model.number="row.requestedQuantity" type="number" min="1" placeholder="수량" />
          <button type="button" class="remove" :disabled="rows.length === 1" @click="removeRow(index)">삭제</button>
        </div>
        <button type="button" class="add-row" @click="addRow">+ 상품 추가</button>

        <label class="field">
          <span>긴급도</span>
          <select v-model="urgency">
            <option value="LOW">낮음</option>
            <option value="NORMAL">보통</option>
            <option value="HIGH">긴급</option>
          </select>
        </label>

        <label class="field">
          <span>신청 사유</span>
          <textarea v-model="requestReason" rows="3" placeholder="예: 프로모션 사전 확보"></textarea>
        </label>

        <p v-if="error" class="error">{{ error }}</p>
      </div>

      <footer>
        <button type="button" class="cancel" @click="$emit('close')">취소</button>
        <button type="button" class="submit" :disabled="submitting" @click="submit">
          {{ submitting ? '신청 중...' : '신청하기' }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createStockRequest } from '../../api/branchStockRequest'

const props = defineProps({
  inventoryItems: { type: Array, required: true },
  presetFlavorId: { type: Number, default: null }
})
const emit = defineEmits(['close', 'submitted'])

const rows = ref([{ flavorId: props.presetFlavorId, requestedQuantity: 10 }])
const urgency = ref('NORMAL')
const requestReason = ref('')
const submitting = ref(false)
const error = ref('')

function addRow() {
  rows.value.push({ flavorId: null, requestedQuantity: 10 })
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

async function submit() {
  error.value = ''
  const items = rows.value.filter((r) => r.flavorId && r.requestedQuantity > 0)
  if (items.length === 0) {
    error.value = '신청할 상품과 수량을 입력해주세요'
    return
  }

  submitting.value = true
  try {
    await createStockRequest({
      requestReason: requestReason.value,
      urgency: urgency.value,
      items: items.map((r) => ({ flavorId: r.flavorId, requestedQuantity: r.requestedQuantity }))
    })
    emit('submitted')
  } catch (e) {
    error.value = e.response?.data?.message ?? '신청에 실패했습니다'
  } finally {
    submitting.value = false
  }
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
  width: 420px;
  max-height: 85vh;
  overflow-y: auto;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

header h3 {
  margin: 0;
}

.close {
  border: none;
  background: transparent;
  font-size: 1.25rem;
  cursor: pointer;
  color: #9ca3af;
}

.row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.row select {
  flex: 2;
}

.row input {
  flex: 1;
  width: 0;
}

.row select,
.row input,
textarea,
.field select {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
}

.remove {
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  padding: 0 0.5rem;
  cursor: pointer;
  color: #dc2626;
}

.remove:disabled {
  opacity: 0.4;
  cursor: default;
}

.add-row {
  border: 1px dashed #c7c9f2;
  background: #f5f5ff;
  color: #4f46e5;
  border-radius: 6px;
  padding: 0.375rem;
  width: 100%;
  cursor: pointer;
  margin-bottom: 0.75rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 0.75rem;
  font-size: 0.8125rem;
  color: #374151;
}

.error {
  color: #dc2626;
  font-size: 0.8125rem;
}

footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  border-top: 1px solid #f1f1f4;
  padding-top: 0.75rem;
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
  cursor: default;
}
</style>
