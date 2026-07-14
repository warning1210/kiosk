<template>
  <!--
    화면 전체를 덮는 모달 배경이다.
    @click.self는 자식인 modal이 아니라 배경 자체를 클릭했을 때만 close 이벤트를 보낸다.
  -->
  <div class="overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>반려 사유</h3>

      <!-- v-model이 textarea 입력값과 script의 reason ref를 양방향으로 연결한다. -->
      <textarea v-model="reason" rows="3" placeholder="반려 사유를 입력해주세요"></textarea>

      <!-- 비어 있는 사유로 제출했을 때만 검증 메시지를 표시한다. -->
      <p v-if="error" class="error">{{ error }}</p>

      <!-- 부모가 API를 처리하는 동안 submitting이 true가 되어 중복 제출을 막는다. -->
      <footer>
        <button type="button" class="cancel" @click="$emit('close')">취소</button>
        <button type="button" class="submit" :disabled="submitting" @click="submit">반려 처리</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// submitting은 부모 목록 화면이 관리하는 API 요청 상태다.
// script에서 직접 읽지 않아 변수에 담지 않아도, <script setup>의 prop은 template에서 이름으로 사용할 수 있다.
defineProps({
  submitting: { type: Boolean, default: false }
})

// 모달은 API를 직접 호출하지 않는다.
// close는 닫기 요청, confirm은 검증된 반려 사유를 부모에게 전달하는 이벤트다.
const emit = defineEmits(['close', 'confirm'])

// 사용자가 입력하는 값과 이 모달 안에서 보여 줄 검증 오류를 반응형 상태로 관리한다.
// 값이 바뀌는 즉시 v-model이 ref를 갱신하므로 별도의 watch는 필요하지 않다.
const reason = ref('')
const error = ref('')

// 공백뿐인 입력을 거부하고, 앞뒤 공백을 제거한 유효한 사유만 부모에게 전달한다.
function submit() {
  if (!reason.value.trim()) {
    error.value = '반려 사유를 입력해주세요'
    return
  }

  // 부모의 onReject가 confirm을 받아 실제 반려 API 호출과 목록 갱신을 담당한다.
  emit('confirm', reason.value.trim())
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

/* 반려 사유 입력에 필요한 작은 크기의 대화상자 */
.modal {
  width: 360px;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
}

h3 {
  margin: 0 0 0.75rem;
}

/* textarea가 padding을 포함해 모달 너비 안에 정확히 들어가게 한다. */
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

/* 취소와 반려 처리 버튼을 오른쪽에 나란히 배치한다. */
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

/* 반려처럼 주의가 필요한 동작은 붉은색으로 구분한다. */
.submit {
  border: none;
  background: #dc2626;
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
