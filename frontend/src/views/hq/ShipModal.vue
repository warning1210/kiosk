<template>
  <!--
    모달 바깥 배경을 클릭하면 close 이벤트를 보내되,
    @click.self 덕분에 모달 내부 입력 영역을 클릭했을 때는 닫히지 않는다.
  -->
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>배송 등록</h3>

      <!--
        각 input의 v-model은 해당 ref와 입력값을 양방향으로 연결한다.
        운송장번호는 필수이고 나머지 배송 정보는 선택 입력이다.
      -->
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

      <!-- 필수 입력 검증에 실패한 경우 사용자에게 이유를 알려 준다. -->
      <p v-if="error" class="error">{{ error }}</p>

      <!-- 부모가 배송 API를 처리하는 동안 제출 버튼을 잠가 중복 등록을 방지한다. -->
      <footer>
        <button type="button" class="cancel" @click="$emit('close')">취소</button>
        <button type="button" class="submit" :disabled="submitting" @click="submit">배송 등록</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// submitting은 부모가 소유한 API 요청 상태이며, template의 disabled 속성에 사용한다.
defineProps({
  submitting: { type: Boolean, default: false }
})

// close는 모달 닫기 요청, confirm은 검증을 마친 배송 정보 전달에 사용한다.
const emit = defineEmits(['close', 'confirm'])

// 각 입력칸을 독립된 ref로 관리한다. v-model이 ref를 바로 갱신하므로 별도 watch는 필요하지 않다.
const trackingNumber = ref('')
const courierName = ref('')
const driverName = ref('')
const estimatedArrivalAt = ref('')
const error = ref('')

// 필수 운송장번호를 검사한 뒤 백엔드 요청 형식의 객체를 부모에게 전달한다.
function submit() {
  if (!trackingNumber.value.trim()) {
    error.value = '운송장번호를 입력해주세요'
    return
  }

  // 문자열은 앞뒤 공백을 제거하고, 선택 입력의 빈 문자열은 명시적인 null로 바꾼다.
  // 부모의 onShip이 이 객체를 받아 실제 배송 등록 API와 목록 갱신을 담당한다.
  emit('confirm', {
    trackingNumber: trackingNumber.value.trim(),
    courierName: courierName.value.trim() || null,
    driverName: driverName.value.trim() || null,
    estimatedArrivalAt: estimatedArrivalAt.value || null
  })
}
</script>

<style scoped>
/* 화면 전체를 덮는 반투명 배경과 모달 중앙 정렬 */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 15, 25, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

/* 배송 정보 입력에 필요한 작은 크기의 대화상자 */
.modal {
  width: 360px;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
}

h3 {
  margin: 0 0 0.75rem;
}

/* 라벨과 입력칸을 세로로 묶고 입력 항목 사이 간격을 만든다. */
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

/* 필수 입력 검증 오류 */
.error {
  color: #dc2626;
  font-size: 0.8125rem;
  margin: 0.375rem 0 0;
}

/* 취소와 배송 등록 버튼을 오른쪽에 나란히 배치한다. */
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

/* 배송 등록은 본점의 주요 실행 버튼 색상으로 강조한다. */
.submit {
  border: none;
  background: #4f46e5;
  color: white;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  cursor: pointer;
}

.submit:disabled {
  /* API 처리 중인 버튼임을 시각적으로도 표시한다. */
  opacity: 0.6;
}
</style>
