<template>
  <!-- 4단계: 상품정보 (컵/콘 둘 다 가능한 상품 → 컵/콘 선택, 테이크아웃 대용량 상품 → 숟가락/드라이아이스 선택) -->
  <!-- 쉬운모드 여부를 화면 루트에 직접 표시해 용기 카드 확대 스타일을 안정적으로 적용한다. -->
  <div class="page" :class="{ 'easy-mode-page': orderFlow.easyMode }">
    <button type="button" class="icon-btn close-btn" :aria-label="t('goHome')" @click="orderFlow.goHome">
      <span v-html="closeXSvg"></span>
    </button>

    <nav class="tab-bar">
      <button type="button" class="tab active">{{ t('productInfo') }}</button>
      <button type="button" class="tab" @click="orderFlow.proceedPastContainer">{{ t('flavor') }}</button>
    </nav>

    <div class="content">
      <div class="product-info">
        <div v-if="isCupConeMode" class="preview-images">
          <img :src="containerCup" alt="" class="preview-img preview-img--cup" />
          <img :src="containerCone" alt="" class="preview-img preview-img--cone" />
        </div>
        <img v-else-if="orderFlow.selectedProduct.imageUrl || productImage(orderFlow.selectedProduct.productName)" :src="orderFlow.selectedProduct.imageUrl || productImage(orderFlow.selectedProduct.productName)" alt="" class="preview-single" />
        <div class="product-text">
          <p class="product-name">
            {{ menuName(orderFlow.selectedProduct.productName) }}<br />
            <template v-if="isCupConeMode">({{ t('cone') }}/{{ t('cup') }})</template>
          </p>
        </div>
        <p class="product-price">₩{{ displayPrice.toLocaleString() }}</p>
      </div>
      <p class="product-desc">{{ t('enjoyProduct').replace('{product}', menuName(orderFlow.selectedProduct.productName)) }}</p>

      <p v-if="isCupConeMode && cart.orderType === 'TAKEOUT'" class="warning-text">★★ {{ t('coneTakeoutWarning') }} ★★</p>

      <div v-if="isCupConeMode" class="options">
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'CUP' }"
          @click="orderFlow.containerType = 'CUP'"
        >
          <img :src="containerCup" alt="" class="option-img" />
          <span class="option-label">{{ t('cup') }}</span>
        </button>
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'CONE' }"
          @click="orderFlow.containerType = 'CONE'"
        >
          <img :src="containerCone" alt="" class="option-img" />
          <span class="option-label">{{ t('cone') }}</span>
        </button>
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'WAFFLE_CONE' }"
          @click="orderFlow.containerType = 'WAFFLE_CONE'"
        >
          <img :src="containerWaffleCone" alt="와플콘" class="option-img" />
          <span class="option-label">{{ t('waffleCone') }}</span>
        </button>
      </div>

      <!-- 포장(테이크아웃) 대용량 상품: 숟가락 개수 / 드라이아이스 시간 -->
      <div v-else class="spoon-dryice-options">
        <div class="option-group">
          <p class="option-group-title">{{ t('spoonCount') }}</p>
          <div class="stepper">
            <button type="button" @click="orderFlow.spoonCount = Math.max(0, orderFlow.spoonCount - 1)">−</button>
            <span class="stepper-value">{{ orderFlow.spoonCount }}</span>
            <button type="button" :disabled="orderFlow.spoonCount >= 10" @click="orderFlow.spoonCount = Math.min(10, orderFlow.spoonCount + 1)">+</button>
          </div>
        </div>
        <div class="option-group">
          <p class="option-group-title">{{ t('dryIceTime') }}</p>
          <div class="dryice-choices">
            <button
              v-for="choice in DRY_ICE_CHOICES"
              :key="String(choice.value)"
              type="button"
              class="dryice-chip"
              :class="{ selected: orderFlow.dryIceMinutes === choice.value }"
              @click="orderFlow.dryIceMinutes = choice.value"
            >
              {{ choice.value === null ? t('notUsed') : `${choice.value}${t('minute')}` }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'product'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>{{ t('previous') }}</span>
      </button>
      <button type="button" class="confirm-btn" @click="orderFlow.proceedPastContainer">{{ t('selectFlavor') }}</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'
import { productImage } from '../../../data/productImages'
import containerCup from '../../../assets/kiosk/icons/container-cup.png'
import containerCone from '../../../assets/kiosk/icons/container-cone.png'
import containerWaffleCone from '../../../assets/kiosk/icons/container-waffle-cone.png'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios-pink.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import { useKioskI18n } from '../../../composables/useKioskI18n'

const closeXSvg = closeXRaw
const orderFlow = useOrderFlowStore()
const cart = useCartStore()
// 키오스크 공통 번역과 메뉴명 번역을 사용합니다.
const { t, menuName } = useKioskI18n()

const isCupConeMode = computed(() => orderFlow.selectedProduct?.containerPolicy === 'CUP_OR_CONE')

// 와플콘을 고르면 담기 전에도 바로 +500원이 보이게 (실제 추가 금액은 orderFlow.addCurrentSelectionToCart와 동일한 규칙)
const displayPrice = computed(
  () => orderFlow.selectedProduct.basePrice + (orderFlow.containerType === 'WAFFLE_CONE' ? 500 : 0)
)

const DRY_ICE_CHOICES = [
  { value: null, label: '사용 안함' },
  { value: 10, label: '10분' },
  { value: 20, label: '20분' },
  { value: 30, label: '30분' }
]
</script>

<style scoped>
.page {
  max-width: 1024px;
  width: 100%;
  margin: 0 auto;
  padding-bottom: 233px;
  background: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.close-btn {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10;
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

.tab-bar {
  display: flex;
  height: 100px;
}

.tab {
  flex: 1;
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

.content {
  padding: 48px 44px 32px;
  display: flex;
  flex-direction: column;
  /* 세로 중앙 정렬 때문에 상단 탭과 상품 정보 사이에 생기던 큰 공백을 없애고 위에서부터 붙여 배치한다. */
  justify-content: flex-start;
  flex: 1;
  min-height: 0;
  gap: 8px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 28px;
}

.preview-images {
  position: relative;
  width: 184px;
  height: 172px;
  flex-shrink: 0;
}

.preview-img {
  position: absolute;
  object-fit: contain;
}

.preview-img--cone {
  width: 118px;
  height: 158px;
  left: 0;
  top: 8px;
  transform: rotate(14deg);
}

.preview-img--cup {
  width: 106px;
  height: 116px;
  left: 80px;
  top: 40px;
  transform: rotate(-13deg);
}

.preview-single {
  width: 140px;
  height: 140px;
  object-fit: contain;
  flex-shrink: 0;
}

.product-text {
  flex: 1;
}

.product-name {
  margin: 0;
  font-size: 32px;
  color: #000;
  line-height: 1.3;
}

.product-price {
  margin: 0;
  font-size: 46px;
  color: #f20c93;
  font-weight: 500;
}

.product-desc {
  margin: 20px 0 0;
  font-size: 26px;
  color: #989898;
}

.warning-text {
  margin: 28px 0 0;
  font-size: 26px;
  color: #f20c0c;
}

.options {
  display: flex;
  gap: 28px;
  margin-top: 44px;
  flex-wrap: wrap;
}

.option-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  width: 250px;
  height: 250px;
  border: 3px solid #d2d2d2;
  border-radius: 26px;
  background: #fff;
  cursor: pointer;
}

.option-card.selected {
  border-color: #ef9bcd;
}

/* 쉬운모드에서는 용기 선택도 2열 큰 카드로 보여 터치 영역을 넓힌다. */
.easy-mode-page .options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  width: 100%;
}

.easy-mode-page .option-card {
  width: 100%;
  height: 300px;
}

.easy-mode-page .option-img {
  width: 160px;
  height: 160px;
}

/* 가져가기에서 파인트 이상 대용량 상품을 누르면 컵/콘 대신 숟가락·드라이아이스 설정이 나온다.
   이 분기에서도 easy-mode-page 값을 그대로 사용해 상품 정보와 모든 터치 영역을 함께 확대한다. */
.easy-mode-page .content {
  padding: 58px 56px 40px;
  gap: 18px;
}

.easy-mode-page .preview-single {
  width: 220px;
  height: 220px;
}

.easy-mode-page .product-info {
  gap: 36px;
}

.easy-mode-page .product-name {
  font-size: 42px;
  font-weight: 700;
}

.easy-mode-page .product-price {
  font-size: 56px;
  font-weight: 700;
}

.easy-mode-page .product-desc {
  margin-top: 28px;
  font-size: 34px;
  line-height: 1.5;
}

.easy-mode-page .spoon-dryice-options {
  gap: 48px;
  margin-top: 48px;
}

.easy-mode-page .option-group-title {
  margin-bottom: 24px;
  font-size: 36px;
  font-weight: 700;
}

/* 숟가락 수량은 원형 버튼 자체와 가운데 숫자를 같이 키워 누르는 위치와 현재 값을 쉽게 구분한다. */
.easy-mode-page .stepper {
  gap: 30px;
}

.easy-mode-page .stepper button {
  width: 90px;
  height: 90px;
  border-width: 2px;
  font-size: 46px;
}

.easy-mode-page .stepper-value {
  min-width: 70px;
  font-size: 44px;
  font-weight: 700;
}

/* 드라이아이스 시간은 글자만 키우면 터치 영역이 그대로이므로 패딩과 최소 높이도 함께 늘린다. */
.easy-mode-page .dryice-choices {
  gap: 18px;
}

.easy-mode-page .dryice-chip {
  min-width: 135px;
  min-height: 82px;
  padding: 18px 30px;
  border-width: 3px;
  border-radius: 30px;
  font-size: 30px;
  font-weight: 700;
}

.option-img {
  width: 124px;
  height: 124px;
  object-fit: contain;
}

.option-label {
  font-size: 28px;
  color: #9f9f9f;
}

.option-card.selected .option-label {
  color: #f20c93;
}

.spoon-dryice-options {
  display: flex;
  flex-direction: column;
  gap: 32px;
  margin-top: 32px;
}

.option-group-title {
  margin: 0 0 18px;
  font-size: 26px;
  color: #000;
}

.stepper {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stepper button {
  width: 62px;
  height: 62px;
  border: 1px solid #d2d2d2;
  border-radius: 50%;
  background: #fff;
  font-size: 28px;
  cursor: pointer;
}

.stepper button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.stepper-value {
  min-width: 52px;
  text-align: center;
  font-size: 32px;
  color: #000;
}

.dryice-choices {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dryice-chip {
  padding: 16px 28px;
  border: 3px solid #d2d2d2;
  border-radius: 26px;
  background: #fff;
  color: #9f9f9f;
  font-size: 24px;
  cursor: pointer;
}

.dryice-chip.selected {
  border-color: #ef9bcd;
  color: #f20c93;
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
</style>
