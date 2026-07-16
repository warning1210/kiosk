<template>
  <!-- 7단계: 포인트/할인 선택 (CU-008) -->
  <div class="page">
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="orderFlow.goHome">
        <span v-html="closeXSvg"></span>
      </button>
    </header>

    <!-- 결제 QR을 한 번 만든 뒤에도 포인트를 다시 조정하고 싶을 수 있어서, 탭을 자유롭게 오갈 수 있게 함
         (STEP02는 QR이 실제로 있어야 볼 내용이 있으므로, qrInfo가 생기기 전까진 비활성) -->
    <nav class="tab-bar">
      <button type="button" class="tab" :class="{ active: activeTab === 'points' }" @click="activeTab = 'points'">
        <span class="tab-badge" :class="{ 'tab-badge--active': activeTab === 'points' }">STEP01</span>
        <span>포인트/할인</span>
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: activeTab === 'payment' }"
        :disabled="!orderFlow.qrInfo"
        @click="activeTab = 'payment'"
      >
        <span class="tab-badge" :class="{ 'tab-badge--active': activeTab === 'payment' }">STEP02</span>
        <span>쿠폰/결제</span>
      </button>
    </nav>

    <div v-if="activeTab === 'points'" class="content">
      <p class="section-title">해피포인트 회원이신가요?</p>

      <div class="point-options">
        <button type="button" class="point-card" :class="{ selected: pointsMode === 'earn' }" @click="openPointFlow('earn')">
          <span class="point-label">적립하기</span>
        </button>
        <button type="button" class="point-card" :class="{ selected: pointsMode === 'use' }" @click="openPointFlow('use')">
          <span class="point-label">사용하기</span>
        </button>
      </div>

      <!-- 고른 모드에 따라 이 부분(중앙)만 바뀐다 -->
      <div v-if="pointsMode === 'earn'" class="point-result">
        <p v-if="orderFlow.isNewMember" class="new-member-hint">
          신규 회원(FRIEND 등급)으로 시작합니다. 결제 완료 시 {{ orderFlow.estimatedEarnedPoints.toLocaleString() }}P가 적립됩니다.
        </p>
        <p v-else-if="orderFlow.customer" class="customer-hint">
          {{ orderFlow.customer.grade }} 등급 · 보유 포인트 {{ orderFlow.customer.pointBalance.toLocaleString() }}P
          (예상 적립 {{ orderFlow.estimatedEarnedPoints.toLocaleString() }}P)
        </p>
      </div>

      <div v-else-if="pointsMode === 'use'" class="point-result">
        <template v-if="orderFlow.customer">
          <p v-if="canUsePoints" class="point-desc">
            보유 포인트 {{ orderFlow.customer.pointBalance.toLocaleString() }}P · 사용 포인트 {{ cart.usedPoints.toLocaleString() }}P
          </p>
          <p v-else class="point-desc">사용 가능한 포인트가 없어요. (100P 이상 보유 시 사용 가능)</p>
        </template>
      </div>
    </div>

    <!-- 해피포인트 카드를 누르면 뜨는 휴대폰 번호 입력 팝업. 조회 후 '사용하기'면 이어서 포인트 입력 단계로 넘어간다 -->
    <div v-if="showKeypad" class="modal-backdrop">
      <div class="modal">
        <button type="button" class="modal-close" aria-label="닫기" @click="showKeypad = false">
          <span v-html="closeXSvg"></span>
        </button>

        <template v-if="phoneStage === 'phone'">
          <h3 class="modal-title">휴대폰 번호 입력</h3>
          <p class="modal-subtitle">포인트 {{ pointsMode === 'use' ? '사용' : '적립' }}을 위해 번호를 입력해주세요.</p>

          <div class="phone-display">
            <span v-if="orderFlow.mobileNumberInput" class="phone-value">{{ phoneDisplay }}</span>
            <span v-else class="phone-placeholder">000-0000-0000</span>
          </div>

          <div class="keypad">
            <button
              v-for="n in ['1', '2', '3', '4', '5', '6', '7', '8', '9']"
              :key="n"
              type="button"
              class="keypad-key"
              @click="appendPhoneDigit(n)"
            >
              {{ n }}
            </button>
            <button type="button" class="keypad-key keypad-key--text" @click="clearPhone">지우기</button>
            <button type="button" class="keypad-key" @click="appendPhoneDigit('0')">0</button>
            <button type="button" class="keypad-key keypad-key--text" aria-label="한 글자 지우기" @click="backspacePhone">⌫</button>
          </div>

          <button
            type="button"
            class="confirm-btn keypad-confirm"
            :disabled="orderFlow.mobileNumberInput.length < PHONE_DIGITS"
            @click="confirmPhoneEntry"
          >
            확인
          </button>
        </template>

        <template v-else-if="phoneStage === 'points'">
          <h3 class="modal-title">사용할 포인트</h3>
          <p v-if="orderFlow.customer" class="modal-subtitle">
            보유 포인트 {{ orderFlow.customer.pointBalance.toLocaleString() }}P
          </p>
          <p v-else class="modal-subtitle">회원 정보를 찾을 수 없어요.</p>

          <template v-if="canUsePoints">
            <p class="used-points">사용 포인트: {{ cart.usedPoints.toLocaleString() }}P</p>
            <div class="point-adjust-buttons">
              <button type="button" @click="orderFlow.adjustUsedPoints(-1000)">-1000</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(-100)">-100</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(100)">+100</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(1000)">+1000</button>
              <button type="button" @click="orderFlow.useMaxPoints">최대금액사용</button>
            </div>
          </template>
          <p v-else class="point-desc">100P 이상 보유 시 사용 가능합니다.</p>

          <button type="button" class="confirm-btn keypad-confirm" @click="showKeypad = false">완료</button>
        </template>
      </div>
    </div>

    <footer class="summary-bar">
      <div class="summary-final">
        <span>최종 결제금액</span>
        <span>₩ {{ cart.totalAmount.toLocaleString() }}</span>
      </div>
      <div class="summary-breakdown">
        <span>총 주문 금액</span>
        <span>₩ {{ cart.amountBeforeDiscount.toLocaleString() }}</span>
        <span class="dash">-</span>
        <span>총 할인 금액</span>
        <span>₩ {{ cart.usedPoints.toLocaleString() }}</span>
      </div>
    </footer>

    <div v-if="activeTab === 'points'" class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'cart'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>이전</span>
      </button>
      <button type="button" class="confirm-btn" :disabled="orderFlow.checkoutInProgress" @click="goToPayment">
        다음단계(결제하기)
      </button>
    </div>
    <p v-if="orderFlow.checkoutError" class="checkout-error">{{ orderFlow.checkoutError }}</p>

    <!-- CU-009: STEP02 - 결제하기를 누르면 팝업이 아니라 이 화면(쿠폰/결제 탭) 안에서 그대로 진행된다 -->
    <div v-if="activeTab === 'payment'" class="content step2-content">
      <h3 class="modal-title">QR 결제</h3>
      <p class="modal-subtitle">휴대폰으로 QR코드를 스캔해주세요.</p>
      <div class="qr-frame">
        <img :src="orderFlow.qrDataUrl" alt="결제 QR코드" width="280" height="280" />
      </div>
      <button type="button" class="test-open-pay" @click="showCheckoutPopup = true">
        (임시 테스트용) 결제 페이지 열기
      </button>

      <div class="timer-badge">
        <img :src="clockIcon" alt="" class="clock-icon" />
        <span>결제 유효 시간</span>
      </div>
      <p class="timer-value">{{ remainingTimeLabel }}</p>
      <p class="timer-note">시간 내 결제가 완료되지 않으면<br />자동 취소됩니다.</p>

      <p class="status-line">상태: {{ orderFlow.paymentStatusLabel }}</p>
      <p v-if="orderFlow.paymentStatus === 'PAID'" class="paid-message">결제가 완료되었습니다. 감사합니다!</p>
      <p v-if="orderFlow.paymentStatus === 'PAID' && !cart.customerMobileNumber" class="paid-message">
        포인트를 적립하지 않으셨습니다. 다음 방문 시 휴대폰 번호를 입력하시면 적립 혜택을 받으실 수 있습니다.
      </p>
      <button v-if="orderFlow.paymentStatus !== 'PAID'" type="button" class="regen-btn" @click="orderFlow.regenerateQr">
        QR코드 재생성
      </button>
      <button type="button" class="cancel-btn" @click="orderFlow.closeQrModal">결제 취소</button>

      <details class="debug-json">
        <summary>결제 요청 JSON</summary>
        <pre>{{ orderFlow.checkoutJsonText }}</pre>
      </details>
    </div>

    <!-- 임시 테스트용: 실제 QR 스캔 없이 결제 화면을 팝업으로 바로 확인 -->
    <div v-if="showCheckoutPopup" class="modal-backdrop">
      <div class="modal">
        <button type="button" class="modal-close" aria-label="닫기" @click="showCheckoutPopup = false">
          <span v-html="closeXSvg"></span>
        </button>
        <CheckoutView :qr-token="orderFlow.qrInfo.qrToken" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import CheckoutView from '../CheckoutView.vue'

import logo from '../../../assets/kiosk/logo.png'
import clockIcon from '../../../assets/kiosk/icons/clock.png'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios-pink.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
const closeXSvg = closeXRaw

const canUsePoints = computed(() => (orderFlow.customer?.pointBalance ?? 0) >= 100)

// STEP01/STEP02 탭 - QR을 만든 뒤에도 포인트를 다시 조정하러 STEP01로 자유롭게 돌아갈 수 있게 별도 상태로 관리
const activeTab = ref('points') // 'points' | 'payment'

function goToPayment() {
  // QR이 이미 있으면(포인트만 다시 보러 왔던 경우) 재요청하지 않고 탭만 이동
  if (orderFlow.qrInfo) {
    activeTab.value = 'payment'
    return
  }
  orderFlow.startPayment()
}

// QR이 새로 생기면 결제 탭으로, 취소/만료 등으로 QR이 사라지면 포인트 탭으로 자동 이동
watch(
  () => orderFlow.qrInfo,
  (qrInfo) => {
    activeTab.value = qrInfo ? 'payment' : 'points'
  }
)

const PHONE_DIGITS = 11 // 010-1234-5678

// 적립하기/사용하기 카드 선택 상태, 그리고 번호 입력 팝업 상태
const pointsMode = ref(null) // 'earn' | 'use' | null
const showKeypad = ref(false)
const phoneStage = ref('phone') // 'phone' -> 확인(조회) -> 'use'면 'points'로 전환

const PHONE_PREFIX = '010'

function openPointFlow(mode) {
  pointsMode.value = mode
  phoneStage.value = 'phone'
  orderFlow.mobileNumberInput = PHONE_PREFIX
  showKeypad.value = true
}

const phoneDisplay = computed(() => {
  const d = orderFlow.mobileNumberInput
  if (d.length <= 3) return d
  if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7, 11)}`
})

function appendPhoneDigit(digit) {
  if (orderFlow.mobileNumberInput.length >= PHONE_DIGITS) return
  orderFlow.mobileNumberInput += digit
}

function backspacePhone() {
  if (orderFlow.mobileNumberInput.length <= PHONE_PREFIX.length) return
  orderFlow.mobileNumberInput = orderFlow.mobileNumberInput.slice(0, -1)
}

// 팝업의 '확인' - 먼저 조회하고, 사용하기를 골랐을 때만 이어서 포인트 입력 단계로 넘어간다
async function confirmPhoneEntry() {
  await orderFlow.lookupCustomer()
  if (pointsMode.value === 'use') {
    phoneStage.value = 'points'
  } else {
    showKeypad.value = false
  }
}

function clearPhone() {
  orderFlow.mobileNumberInput = PHONE_PREFIX
}

// 임시: QR을 휴대폰으로 스캔하기 어려운 개발 환경(localhost)에서 결제 페이지를 팝업으로 바로 테스트하기 위한 상태
const showCheckoutPopup = ref(false)

// QR 유효 시간(백엔드 PaymentService.QR_VALID_MINUTES=5분) 실시간 카운트다운
const now = ref(Date.now())
let timerId = null
watch(
  () => orderFlow.qrInfo,
  (qrInfo) => {
    if (timerId) clearInterval(timerId)
    if (!qrInfo) showCheckoutPopup.value = false
    if (qrInfo) {
      now.value = Date.now()
      timerId = setInterval(() => {
        now.value = Date.now()
      }, 1000)
    }
  },
  { immediate: true }
)
onUnmounted(() => {
  if (timerId) clearInterval(timerId)
})

const remainingTimeLabel = computed(() => {
  if (!orderFlow.qrInfo?.expiresAt) return '00:00'
  const remainingMs = Math.max(0, new Date(orderFlow.qrInfo.expiresAt).getTime() - now.value)
  const totalSeconds = Math.floor(remainingMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
</script>

<style scoped>
.page {
  max-width: 1024px;
  margin: 0 auto;
  padding-bottom: 433px;
  background: #fff;
  min-height: 100vh;
}

.top-bar {
  height: 114px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 27px;
}

.icon-btn {
  width: 53px;
  height: 53px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.close-btn :deep(svg) {
  width: 55px;
  height: 55px;
}

.logo {
  height: 88px;
  width: 96px;
  object-fit: contain;
}

.tab-bar {
  display: flex;
  height: 100px;
}

.tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: none;
  background: #f8f8f8;
  color: #cacaca;
  font-size: 30px;
  font-weight: 500;
  cursor: pointer;
}

.tab.active {
  background: #fff;
  color: #f20c93;
}

.tab:disabled {
  cursor: default;
}

.tab-badge {
  padding: 3px 10px;
  border-radius: 99px;
  background: #cbcbcb;
  color: #fff;
  font-size: 11px;
}

.tab-badge--active {
  background: #f20c93;
}

.content {
  padding: 24px 40px;
}

.step2-content {
  text-align: center;
}

.new-member-hint,
.customer-hint {
  margin: 8px 0 0;
  font-size: 28px;
  color: #f20c93;
  text-align: center;
}

.section-title {
  margin: 24px 0 16px;
  font-size: 35px;
  color: #000;
  text-align: center;
}

.point-options {
  display: flex;
  justify-content: center;
  gap: 32px;
}

.point-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 122px;
  height: 122px;
  border: 1px solid #d2d2d2;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.point-card.selected {
  border-color: #f20c93;
}

.point-label {
  font-size: 15px;
  color: #000;
}

.point-result {
  margin-top: 16px;
}

.phone-display {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64px;
  margin: 0 auto 16px;
  font-size: 36px;
  font-weight: 500;
  letter-spacing: 2px;
}

.phone-value {
  color: #000;
}

.phone-placeholder {
  color: #cacaca;
}

.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  max-width: 360px;
  margin: 0 auto;
}

.keypad-key {
  height: 64px;
  border: 1px solid #e2e2e2;
  border-radius: 12px;
  background: #fafafa;
  color: #000;
  font-size: 24px;
  cursor: pointer;
}

.keypad-key--text {
  font-size: 16px;
  color: #666;
}

.point-desc {
  margin: 16px 0 0;
  font-size: 16px;
  color: #a1a1a1;
  text-align: center;
}

.keypad-confirm {
  display: block;
  width: 100%;
  max-width: 340px;
  margin: 24px auto 0;
  height: auto;
  padding: 16px;
}

.used-points {
  margin: 8px 0;
  font-size: 18px;
  color: #333;
}

.point-adjust-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.point-adjust-buttons button {
  padding: 8px 12px;
  border: 1px solid #d2d2d2;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.summary-bar {
  position: fixed;
  bottom: 233px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  background: #f6f6f6;
  padding: 0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.summary-final {
  display: flex;
  justify-content: space-between;
  padding: 0 40px;
  height: 100px;
  align-items: center;
  color: #f20c93;
  font-size: 30px;
}

.summary-breakdown {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 40px;
  height: 100px;
  color: #000;
  font-size: 20px;
}

.dash {
  color: #999;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  height: 233px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 21px;
  background: #fff;
}

.prev-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 471px;
  height: 114px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #fff;
  color: #f20c93;
  font-size: 30px;
  cursor: pointer;
}

.prev-arrow {
  width: 12px;
  height: 20px;
  transform: rotate(180deg);
}

.confirm-btn {
  width: 471px;
  height: 114px;
  border: 1px solid #b9b9b9;
  border-radius: 99px;
  background: #f20c93;
  color: #fff;
  font-size: 30px;
  cursor: pointer;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.checkout-error {
  position: fixed;
  bottom: 440px;
  left: 50%;
  transform: translateX(-50%);
  color: #f20c0c;
  font-size: 14px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.modal {
  position: relative;
  width: min(637px, 90vw);
  max-height: 90vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 26px;
  padding: 48px 40px;
  text-align: center;
}

.modal-close {
  position: absolute;
  top: 27px;
  right: 27px;
  width: 53px;
  height: 53px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.modal-close :deep(svg) {
  width: 100%;
  height: 100%;
}

.modal-title {
  margin: 0;
  font-size: 50px;
  color: #f20c93;
}

.modal-subtitle {
  margin: 16px 0 24px;
  font-size: 20px;
  color: #000;
}

.qr-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 300px;
  height: 300px;
  margin: 0 auto;
  border: 3px solid #f20c93;
  border-radius: 20px;
}

.qr-frame img {
  width: 260px;
  height: 260px;
}

.test-open-pay {
  display: block;
  margin: 0.75rem auto;
  padding: 0.5rem 0.75rem;
  color: #b8860b;
  border: 1px dashed #b8860b;
  background: #fff8e1;
  font-size: 0.8rem;
  cursor: pointer;
}

.timer-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 260px;
  margin: 16px auto 0;
  padding: 12px;
  border-radius: 20px;
  background: #ffe3f3;
  font-size: 16px;
  color: #000;
}

.clock-icon {
  width: 20px;
  height: 20px;
}

.timer-value {
  margin: 12px 0 0;
  font-size: 36px;
  color: #f20c93;
  font-weight: 500;
}

.timer-note {
  margin: 12px 0 0;
  font-size: 14px;
  color: #a1a1a1;
}

.status-line {
  margin: 16px 0 0;
  font-size: 16px;
  color: #333;
}

.paid-message {
  margin: 8px 0 0;
  font-size: 28px;
  color: #f20c93;
}

.regen-btn,
.cancel-btn {
  display: block;
  width: 100%;
  max-width: 340px;
  margin: 16px auto 0;
  padding: 16px;
  border: 1px solid #b9b9b9;
  border-radius: 40px;
  background: #fff;
  color: #f20c93;
  font-size: 18px;
  cursor: pointer;
}

.debug-json {
  margin-top: 16px;
  text-align: left;
  font-size: 12px;
  color: #999;
}

.debug-json pre {
  max-height: 200px;
  overflow: auto;
  background: #f4f4f4;
  padding: 0.75rem;
  font-size: 0.75rem;
}
</style>
