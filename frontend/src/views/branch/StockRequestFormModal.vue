<template>
  <!--
    모달 바깥의 반투명 영역이다.
    @click.self는 실제로 overlay 자체를 클릭했을 때만 close 이벤트를 보내므로,
    모달 안쪽을 클릭해도 실수로 창이 닫히지 않는다.
  -->
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <!-- 제목과 닫기 버튼 영역 -->
      <header>
        <h3>재고 신청</h3>
        <button type="button" class="close" @click="$emit('close')">×</button>
      </header>

      <div class="body">
        <!--
          rows의 각 원소가 신청 상품 한 줄을 나타낸다.
          배열 순번(index)이 아니라 변하지 않는 rowId를 key로 사용해야 행을 삭제해도
          Vue가 다른 행의 입력 DOM을 잘못 재사용하지 않는다.
          v-model.number는 select/input에서 받은 문자열 숫자를 Number로 바꿔 상태에 저장한다.
        -->
        <div v-for="(row, index) in rows" :key="row.rowId" class="row">
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

        <!-- 모든 신청 상품에 공통으로 적용되는 긴급도와 신청 사유 -->
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

        <!-- 검증 오류 또는 서버가 반환한 오류 메시지는 입력 영역 바로 아래에 보여준다. -->
        <p v-if="error" class="error">{{ error }}</p>
      </div>

      <!-- submitting 중에는 중복 신청을 막기 위해 제출 버튼을 비활성화한다. -->
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

// 부모 화면이 전달하는 재고 목록과, 미리 선택할 맛 ID를 props로 받는다.
// props는 부모가 소유한 읽기 전용 데이터이므로 이 컴포넌트에서 직접 수정하지 않는다.
const props = defineProps({
  inventoryItems: { type: Array, required: true },
  presetFlavorId: { type: Number, default: null }
})

// close는 모달 닫기를, submitted는 신청 완료 후 목록 갱신을 부모에게 요청하는 이벤트다.
const emit = defineEmits(['close', 'submitted'])

// 새 행마다 고유한 값을 부여한다. 단순한 화면 내부 식별자이므로 서버에 전송하지 않는다.
let nextRowId = 1

// 행의 기본 구조를 한곳에서 만들면 최초 행과 추가 행이 항상 같은 필드를 갖는다.
function createRow(flavorId = null) {
  return {
    rowId: nextRowId++,
    flavorId,
    requestedQuantity: 10
  }
}

// ref로 감싼 값이 바뀌면 Vue가 해당 값을 사용하는 화면을 자동으로 다시 그린다.
// script에서는 .value로 접근하고, template에서는 Vue가 자동 해제하므로 이름만 사용한다.
const rows = ref([createRow(props.presetFlavorId)])
const urgency = ref('NORMAL')
const requestReason = ref('')
const submitting = ref(false)
const error = ref('')

// 배열 자체를 교체하지 않아도 push/splice 같은 변경을 Vue가 감지한다.
function addRow() {
  rows.value.push(createRow())
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

// 입력 검증 → API 형식으로 변환 → 서버 요청 → 부모 알림 순서로 신청을 처리한다.
async function submit() {
  error.value = ''

  // 맛이 선택되고 수량이 1개 이상인 행만 실제 신청 항목으로 인정한다.
  const items = rows.value.filter((r) => r.flavorId && r.requestedQuantity > 0)
  if (items.length === 0) {
    error.value = '신청할 상품과 수량을 입력해주세요'
    return
  }

  submitting.value = true
  try {
    // rowId는 화면 렌더링용이므로 제외하고 백엔드가 받는 DTO 필드만 전송한다.
    await createStockRequest({
      requestReason: requestReason.value,
      urgency: urgency.value,
      items: items.map((r) => ({ flavorId: r.flavorId, requestedQuantity: r.requestedQuantity }))
    })

    // 성공한 뒤 부모가 모달을 닫거나 신청 목록을 새로 불러올 수 있게 알린다.
    emit('submitted')
  } catch (e) {
    // 서버가 업무 오류 메시지를 주면 우선 사용하고, 없으면 공통 안내 문구를 사용한다.
    error.value = e.response?.data?.message ?? '신청에 실패했습니다'
  } finally {
    // 성공과 실패 어느 경우에도 버튼을 다시 사용할 수 있도록 상태를 복구한다.
    submitting.value = false
  }
}
</script>

<style scoped>
/* 화면 전체를 덮는 배경과 모달 중앙 정렬 */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 15, 25, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

/* 입력 행이 많아져도 화면 높이를 넘지 않도록 모달 내부에서 스크롤한다. */
.modal {
  width: 420px;
  max-height: 85vh;
  overflow-y: auto;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
}

/* 모달 제목과 닫기 버튼 */
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

/* 한 행 안에 맛 선택, 수량, 삭제 버튼을 가로로 배치한다. */
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

/* 행 삭제와 상품 추가 버튼의 상태 표현 */
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

/* 긴급도와 신청 사유를 세로 형태의 입력 묶음으로 표시한다. */
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

/* 모달 하단의 취소/제출 동작 영역 */
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
  /* 서버 요청 중이라는 사실을 시각적으로도 알린다. */
  opacity: 0.6;
  cursor: default;
}
</style>
