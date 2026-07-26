<template>
  <!-- 7단계: 쿠폰/포인트(휴대폰 번호로 함께 조회) -> 결제 (CU-008) -->
  <div class="page" :class="{ 'page--no-actions': activeTab === 'payment' }">
    <header class="top-bar">
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <button type="button" class="icon-btn close-btn" :aria-label="t('goHome')" @click="orderFlow.goHome">
        <span v-html="closeXSvg"></span>
      </button>
    </header>

    <!-- 결제 QR을 한 번 만든 뒤에도 쿠폰/포인트를 다시 조정하고 싶을 수 있어서, 탭을 자유롭게 오갈 수 있게 함
         (STEP02는 QR이 실제로 있어야 볼 내용이 있으므로, qrInfo가 생기기 전까진 비활성) -->
    <nav class="tab-bar">
      <button type="button" class="tab" :class="{ active: activeTab === 'main' }" @click="activeTab = 'main'">
        <span class="tab-stack">
          <span class="tab-badge" :class="{ 'tab-badge--active': activeTab === 'main' }">STEP01</span>
          <span>{{ t('pointsDiscount') }}</span>
        </span>
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: activeTab === 'payment' }"
        :disabled="!orderFlow.qrInfo"
        @click="activeTab = 'payment'"
      >
        <span class="tab-stack">
          <span class="tab-badge" :class="{ 'tab-badge--active': activeTab === 'payment' }">STEP02</span>
          <span>{{ t('couponPayment') }}</span>
        </span>
      </button>
    </nav>

    <div v-if="activeTab === 'main'" class="content">
      <template v-if="!orderFlow.customerLookupDone">
        <p class="section-title">{{ t('pointsDiscount') }}</p>
        <p class="coupon-hint">{{ t('enterPhone') }}</p>
        <button type="button" class="lookup-btn" @click="openLookup">{{ t('enterPhone') }}</button>
      </template>

      <template v-else>
        <!-- 번호를 잘못 입력했거나 다른 회원으로 바꾸고 싶을 때 다시 입력할 수 있게 함 -->
        <div class="phone-recheck">
          <span class="phone-recheck-value">{{ phoneDisplay }}</span>
          <button type="button" class="phone-recheck-btn" @click="openLookup">{{ t('enterPhone') }}</button>
        </div>

        <p class="section-title">사용 가능한 쿠폰</p>
        <p v-if="!orderFlow.customer?.coupons?.length" class="coupon-hint">사용 가능한 쿠폰이 없어요.</p>
        <ul v-else class="coupon-list">
          <li v-for="coupon in orderFlow.customer.coupons" :key="coupon.couponId" class="coupon-item">
            <div class="coupon-item-info">
              <span class="coupon-item-name">{{ coupon.couponName }}</span>
              <span class="coupon-item-desc">{{ couponDiscountLabel(coupon) }} · {{ coupon.expiresAt.slice(0, 10) }}까지</span>
            </div>
            <button
              type="button"
              class="coupon-use-btn"
              :class="{ selected: cart.couponCode === coupon.qrToken }"
              :disabled="couponChecking"
              @click="toggleCoupon(coupon)"
            >
              {{ cart.couponCode === coupon.qrToken ? '선택됨' : '사용' }}
            </button>
          </li>
        </ul>
        <p v-if="couponError" class="coupon-error">{{ couponError }}</p>

        <p class="section-title">포인트</p>
        <p v-if="cart.couponDiscount > 0" class="coupon-applied-hint">
          쿠폰 할인 {{ cart.couponDiscount.toLocaleString() }}원이 적용된 금액 기준으로 적립됩니다.
        </p>

        <div class="point-options">
          <button type="button" class="point-card" :class="{ selected: pointsMode === 'earn' }" @click="openPointFlow('earn')">
            <span class="point-label">{{ t('earnPoints') }}</span>
          </button>
          <button type="button" class="point-card" :class="{ selected: pointsMode === 'use' }" @click="openPointFlow('use')">
            <span class="point-label">{{ t('usePoints') }}</span>
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
      </template>
    </div>

    <!-- 조회하기/해피포인트 카드를 누르면 뜨는 휴대폰 번호 입력 팝업. 조회 후 '사용하기'면 이어서 포인트 입력 단계로 넘어간다 -->
    <div v-if="showKeypad" class="modal-backdrop">
      <div class="modal">
        <button type="button" class="modal-close" :aria-label="t('close')" @click="showKeypad = false">
          <span v-html="closeXSvg"></span>
        </button>

        <template v-if="phoneStage === 'phone'">
          <h3 class="modal-title">{{ t('enterPhone') }}</h3>
          <p class="modal-subtitle">{{ t('enterPhoneHint').replace('{mode}', t('pointsDiscount')) }}</p>

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
            <button type="button" class="keypad-key keypad-key--text" @click="clearPhone">{{ t('clear') }}</button>
            <button type="button" class="keypad-key" @click="appendPhoneDigit('0')">0</button>
            <button type="button" class="keypad-key keypad-key--text" aria-label="한 글자 지우기" @click="backspacePhone">⌫</button>
          </div>

          <button
            type="button"
            class="confirm-btn keypad-confirm"
            :disabled="orderFlow.mobileNumberInput.length < PHONE_DIGITS"
            @click="confirmPhoneEntry"
          >
            {{ t('confirm') }}
          </button>
        </template>

        <template v-else-if="phoneStage === 'points'">
          <h3 class="modal-title">{{ t('pointsToUse') }}</h3>
          <p v-if="orderFlow.customer" class="modal-subtitle">
            {{ t('availablePoints') }} {{ orderFlow.customer.pointBalance.toLocaleString() }}P
          </p>
          <p v-else class="modal-subtitle">{{ t('memberNotFound') }}</p>

          <template v-if="canUsePoints">
            <p class="used-points">{{ t('usedPoints') }}: {{ cart.usedPoints.toLocaleString() }}P</p>
            <div class="point-adjust-buttons">
              <button type="button" @click="orderFlow.adjustUsedPoints(-1000)">-1000</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(-100)">-100</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(100)">+100</button>
              <button type="button" @click="orderFlow.adjustUsedPoints(1000)">+1000</button>
              <button type="button" @click="orderFlow.useMaxPoints">{{ t('useMaximum') }}</button>
            </div>
          </template>
          <p v-else class="point-desc">{{ t('pointsMinimum') }}</p>

          <button type="button" class="confirm-btn keypad-confirm" @click="showKeypad = false">{{ t('done') }}</button>
        </template>
      </div>
    </div>

    <footer class="summary-bar" :class="{ 'summary-bar--no-actions': activeTab === 'payment' }">
      <div class="summary-final">
        <span>{{ t('finalAmount') }}</span>
        <span>₩ {{ cart.totalAmount.toLocaleString() }}</span>
      </div>
      <div class="summary-breakdown">
        <span>{{ t('orderAmount') }}</span>
        <span>₩ {{ cart.amountBeforeDiscount.toLocaleString() }}</span>
        <span class="dash">-</span>
        <span>{{ t('discountAmount') }}</span>
        <span>₩ {{ (cart.usedPoints + cart.couponDiscount).toLocaleString() }}</span>
      </div>
    </footer>

    <div v-if="activeTab !== 'payment'" class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'cart'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>{{ t('previous') }}</span>
      </button>
      <button type="button" class="confirm-btn" :disabled="orderFlow.checkoutInProgress" @click="goToPayment">
        {{ t('nextPayment') }}
      </button>
    </div>
    <p v-if="orderFlow.checkoutError" class="checkout-error">{{ orderFlow.checkoutError }}</p>

    <!-- CU-009: STEP02 - 결제하기를 누르면 팝업이 아니라 이 화면(결제 탭) 안에서 그대로 진행된다 -->
    <div v-if="activeTab === 'payment'" class="content step2-content">
      <h3 class="modal-title">{{ t('qrPayment') }}</h3>
      <p class="modal-subtitle">{{ t('scanQr') }}</p>
      <div class="qr-frame">
        <img :src="orderFlow.qrDataUrl" alt="결제 QR코드" width="280" height="280" />
      </div>
      <button type="button" class="test-open-pay" @click="openPaymentPopup">
        (임시 테스트용) 결제 페이지 열기
      </button>

      <div class="timer-badge">
        <img :src="clockIcon" alt="" class="clock-icon" />
        <span>{{ t('paymentTime') }}</span>
      </div>
      <p class="timer-value">{{ remainingTimeLabel }}</p>
      <p class="timer-note">{{ t('paymentExpiry') }}</p>

      <p class="status-line">{{ t('status') }}: {{ orderFlow.paymentStatusLabel }}</p>
      <p v-if="orderFlow.paymentStatus === 'PAID'" class="paid-message">{{ t('paymentComplete') }}</p>
      <p v-if="orderFlow.paymentStatus === 'PAID' && !cart.customerMobileNumber" class="paid-message">
        포인트를 적립하지 않으셨습니다. 다음 방문 시 휴대폰 번호를 입력하시면 적립 혜택을 받으실 수 있습니다.
      </p>
      <button v-if="orderFlow.paymentStatus !== 'PAID'" type="button" class="regen-btn" @click="orderFlow.regenerateQr">
        {{ t('regenerateQr') }}
      </button>
      <button type="button" class="cancel-btn" @click="orderFlow.closeQrModal">{{ t('cancelPayment') }}</button>

      <details class="debug-json">
        <summary>결제 요청 JSON</summary>
        <pre>{{ orderFlow.checkoutJsonText }}</pre>
      </details>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import http from '../../../api/http'
import { useKioskI18n } from '../../../composables/useKioskI18n'

import logo from '../../../assets/kiosk/logo.png'
import clockIcon from '../../../assets/kiosk/icons/clock.png'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios-pink.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'

const orderFlow = useOrderFlowStore()
// 현금/신용카드 선택 이후의 포인트·쿠폰·QR 화면도 선택 언어를 공유합니다.
const { t } = useKioskI18n()
const cart = useCartStore()
const closeXSvg = closeXRaw

const canUsePoints = computed(() => (orderFlow.customer?.pointBalance ?? 0) >= 100)
const couponChecking = ref(false)
const couponError = ref('')

function couponDiscountLabel(coupon) {
  return coupon.discountType === 'RATE'
    ? `${Number(coupon.discountRate)}% 할인`
    : `${coupon.discountAmount.toLocaleString()}원 할인`
}

// 쿠폰 목록의 '사용' 버튼 - 이미 선택된 쿠폰을 다시 누르면 해제, 다른 쿠폰을 누르면 그걸로 교체(필드가 하나라 최대 1개는 자연히 보장)
async function toggleCoupon(coupon) {
  couponError.value = ''
  if (cart.couponCode === coupon.qrToken) {
    cart.setCouponCode(null)
    cart.setCouponDiscount(0)
    return
  }
  couponChecking.value = true
  try {
    const { data } = await http.get('/coupons/check', {
      params: { code: coupon.qrToken, amount: cart.amountBeforeDiscount }
    })
    cart.setCouponCode(coupon.qrToken)
    cart.setCouponDiscount(data.discountAmount)
  } catch (e) {
    cart.setCouponDiscount(0)
    couponError.value = e.response?.data?.message || '쿠폰을 사용할 수 없습니다.'
  } finally {
    couponChecking.value = false
  }
}

// STEP01(쿠폰/포인트)/STEP02(결제) 탭 - QR을 만든 뒤에도 STEP01로 자유롭게 돌아갈 수 있게 별도 상태로 관리
const activeTab = ref('main') // 'main' | 'payment'

function goToPayment() {
  // QR이 이미 있으면(쿠폰/포인트만 다시 보러 왔던 경우) 재요청하지 않고 탭만 이동
  if (orderFlow.qrInfo) {
    activeTab.value = 'payment'
    return
  }
  orderFlow.startPayment()
}

// QR이 새로 생기면 결제 탭으로, 취소/만료 등으로 QR이 사라지면 쿠폰/포인트 탭으로 자동 이동
watch(
  () => orderFlow.qrInfo,
  (qrInfo) => {
    activeTab.value = qrInfo ? 'payment' : 'main'
  }
)

const PHONE_DIGITS = 11 // 010-1234-5678

// 적립하기/사용하기 카드 선택 상태, 그리고 번호 입력 팝업 상태
const pointsMode = ref(null) // 'earn' | 'use' | null
const showKeypad = ref(false)
const phoneStage = ref('phone') // 'phone' -> 확인(조회) -> 'use'면 'points'로 전환

const PHONE_PREFIX = '010'

// STEP01 진입 시 쿠폰/포인트를 아직 조회하지 않았을 때 보이는 "휴대폰 번호로 조회하기" 버튼
function openLookup() {
  phoneStage.value = 'phone'
  orderFlow.mobileNumberInput = PHONE_PREFIX
  showKeypad.value = true
}

function openPointFlow(mode) {
  // 신규 회원(조회했지만 보유 포인트 없음)은 사용할 포인트가 없다 - 안내만 띄우고 적립하기 선택은 그대로 둔다
  if (mode === 'use' && orderFlow.isNewMember) {
    orderFlow.showNotice('사용 가능한 포인트가 없어요. 이번 결제로 포인트를 적립해보세요!')
    return
  }

  pointsMode.value = mode
  // 적립하기로 바꾸면 이전에 잡아둔 사용 포인트는 무효(적립 대상에서 제외되므로) - 초기화해서 헷갈리지 않게 한다
  if (mode === 'earn') cart.setUsedPoints(0)
  // 적립/사용 중 한쪽에서 번호 조회를 이미 마쳤다면, 다른 쪽으로 바꿀 때 번호를 다시 묻지 않는다
  if (orderFlow.customerLookupDone) {
    if (mode === 'use') {
      phoneStage.value = 'points'
      showKeypad.value = true
    }
    return
  }
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
  // 신규 회원은 사용할 포인트가 없으므로, 사용하기로 조회했더라도 적립하기로 전환한다
  if (pointsMode.value === 'use' && orderFlow.isNewMember) {
    pointsMode.value = 'earn'
    showKeypad.value = false
    return
  }
  if (pointsMode.value === 'use') {
    phoneStage.value = 'points'
  } else {
    showKeypad.value = false
  }
}

function clearPhone() {
  orderFlow.mobileNumberInput = PHONE_PREFIX
}

// 임시 테스트용: 휴대폰으로 QR을 스캔하는 대신, 같은 PC에서 결제 페이지를 별도 창(새 탭)으로 띄운다.
// CustomerPaymentStep 안에 인라인 모달로 띄우면 토스 결제 후 successUrl로 "이 탭 자체"가 리다이렉트되어
// 키오스크 화면(주문 상태 폴링)이 통째로 날아가 버리므로, 반드시 별도 창으로 열어야 한다.
function openPaymentPopup() {
  const url = `${window.location.origin}/pay/${orderFlow.qrInfo.qrToken}`
  window.open(url, 'kioskPaymentTest', 'width=720,height=800')
}

// QR 유효 시간(백엔드 PaymentService.QR_VALID_MINUTES=5분) 실시간 카운트다운
const now = ref(Date.now())
let timerId = null
watch(
  () => orderFlow.qrInfo,
  (qrInfo) => {
    if (timerId) clearInterval(timerId)
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
  width: 100%;
  margin: 0 auto;
  padding-bottom: 433px;
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* STEP03(QR결제) 탭은 .bottom-bar가 없으니 그만큼(233px) 여백을 덜 잡는다 */
.page--no-actions {
  padding-bottom: 200px;
}

.top-bar {
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 27px;
}

.icon-btn {
  width: 62px;
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.close-btn :deep(svg) {
  width: 64px;
  height: 64px;
}

.logo {
  height: 100px;
  width: 112px;
  object-fit: contain;
}

.tab-bar {
  display: flex;
  height: 136px;
}

.tab {
  flex: 1;
  border: none;
  background: #f8f8f8;
  color: #cacaca;
  font-size: 32px;
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

.tab-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.tab-badge {
  padding: 5px 16px;
  border-radius: 99px;
  background: #cbcbcb;
  color: #fff;
  font-size: 16px;
}

.tab-badge--active {
  background: #f20c93;
}

.content {
  padding: 32px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex: 1;
  min-height: 0;
  gap: 8px;
}

.step2-content {
  text-align: center;
}

.new-member-hint,
.customer-hint {
  margin: 16px 0 0;
  font-size: 30px;
  color: #f20c93;
  text-align: center;
}

.section-title {
  margin: 44px 0 28px;
  font-size: 48px;
  color: #000;
  text-align: center;
}

.coupon-applied-hint {
  margin: -8px 0 24px;
  font-size: 22px;
  color: #f20c93;
  text-align: center;
}

.point-options {
  display: flex;
  justify-content: center;
  gap: 52px;
}

.point-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 220px;
  height: 220px;
  border: 3px solid #d2d2d2;
  border-radius: 22px;
  background: #fff;
  cursor: pointer;
}

.point-card.selected {
  border-color: #f20c93;
}

.point-label {
  font-size: 28px;
  color: #000;
}

.point-result {
  margin-top: 32px;
}

.phone-recheck {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 24px;
  font-size: 28px;
  color: #666;
}

.phone-recheck-btn {
  padding: 14px 28px;
  border: 1px solid #f20c93;
  border-radius: 99px;
  background: #fff;
  color: #f20c93;
  font-size: 24px;
  cursor: pointer;
}

.coupon-hint {
  margin: 16px 0 0;
  font-size: 26px;
  color: #a1a1a1;
  text-align: center;
}

.coupon-error {
  margin: 16px 0 0;
  font-size: 24px;
  color: #f20c0c;
  text-align: center;
}

.lookup-btn {
  display: block;
  margin: 32px auto 0;
  padding: 26px 56px;
  border: 1px solid #f20c93;
  border-radius: 99px;
  background: #f20c93;
  color: #fff;
  font-size: 30px;
  cursor: pointer;
}

.coupon-list {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.coupon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 32px;
  border: 2px solid #d2d2d2;
  border-radius: 18px;
}

.coupon-item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.coupon-item-name {
  font-size: 28px;
  color: #000;
}

.coupon-item-desc {
  font-size: 20px;
  color: #a1a1a1;
}

.coupon-use-btn {
  flex-shrink: 0;
  padding: 18px 34px;
  border: 1px solid #f20c93;
  border-radius: 99px;
  background: #fff;
  color: #f20c93;
  font-size: 24px;
  cursor: pointer;
}

.coupon-use-btn.selected {
  background: #f20c93;
  color: #fff;
}

.coupon-use-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.phone-display {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100px;
  margin: 0 auto 24px;
  font-size: 56px;
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
  gap: 16px;
  max-width: 520px;
  margin: 0 auto;
}

.keypad-key {
  height: 100px;
  border: 1px solid #e2e2e2;
  border-radius: 20px;
  background: #fafafa;
  color: #000;
  font-size: 34px;
  cursor: pointer;
}

.keypad-key--text {
  font-size: 24px;
  color: #666;
}

.point-desc {
  margin: 24px 0 0;
  font-size: 26px;
  color: #a1a1a1;
  text-align: center;
}

.keypad-confirm {
  display: block;
  width: 100%;
  max-width: 480px;
  margin: 32px auto 0;
  height: auto;
  padding: 26px;
}

.used-points {
  margin: 16px 0;
  font-size: 28px;
  color: #333;
}

.point-adjust-buttons {
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.point-adjust-buttons button {
  padding: 18px 26px;
  border: 1px solid #d2d2d2;
  border-radius: 16px;
  background: #fff;
  font-size: 22px;
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

/* STEP03(QR결제) 탭은 .bottom-bar가 렌더링되지 않으므로, 그 자리(233px)를 비워두지 않고 화면 맨 아래에 붙인다 */
.summary-bar--no-actions {
  bottom: 0;
}

.summary-final {
  display: flex;
  justify-content: space-between;
  padding: 0 40px;
  height: 100px;
  align-items: center;
  color: #f20c93;
  font-size: 44px;
}

.summary-breakdown {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 40px;
  height: 100px;
  color: #000;
  font-size: 28px;
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
  width: min(760px, 92vw);
  max-height: 90vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 30px;
  padding: 56px 48px;
  text-align: center;
}

.modal-close {
  position: absolute;
  top: 30px;
  right: 30px;
  width: 60px;
  height: 60px;
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
  font-size: 56px;
  color: #f20c93;
}

.modal-subtitle {
  margin: 20px 0 32px;
  font-size: 30px;
  color: #000;
}

.qr-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 440px;
  height: 440px;
  margin: 0 auto;
  border: 5px solid #f20c93;
  border-radius: 28px;
}

.qr-frame img {
  width: 400px;
  height: 400px;
}

.test-open-pay {
  display: block;
  margin: 1rem auto;
  padding: 0.6rem 0.9rem;
  color: #b8860b;
  border: 1px dashed #b8860b;
  background: #fff8e1;
  font-size: 0.95rem;
  cursor: pointer;
}

.timer-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 400px;
  margin: 28px auto 0;
  padding: 20px;
  border-radius: 30px;
  background: #ffe3f3;
  font-size: 26px;
  color: #000;
}

.clock-icon {
  width: 30px;
  height: 30px;
}

.timer-value {
  margin: 20px 0 0;
  font-size: 56px;
  color: #f20c93;
  font-weight: 500;
}

.timer-note {
  margin: 18px 0 0;
  font-size: 24px;
  color: #a1a1a1;
}

.status-line {
  margin: 24px 0 0;
  font-size: 26px;
  color: #333;
}

.paid-message {
  margin: 16px 0 0;
  font-size: 34px;
  color: #f20c93;
}

.regen-btn,
.cancel-btn {
  display: block;
  width: 100%;
  max-width: 480px;
  margin: 24px auto 0;
  padding: 26px;
  border: 1px solid #b9b9b9;
  border-radius: 54px;
  background: #fff;
  color: #f20c93;
  font-size: 28px;
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
