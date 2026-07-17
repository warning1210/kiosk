<template>
  <!-- 4단계: 상품정보 (컵/콘 둘 다 가능한 상품 → 컵/콘 선택, 테이크아웃 대용량 상품 → 숟가락/드라이아이스 선택) -->
  <div class="page">
    <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="orderFlow.goHome">
      <span v-html="closeXSvg"></span>
    </button>

    <nav class="tab-bar">
      <button type="button" class="tab active">상품정보</button>
      <button type="button" class="tab" @click="orderFlow.proceedPastContainer">플레이버</button>
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
            {{ orderFlow.selectedProduct.productName }}<br />
            <template v-if="isCupConeMode">(콘/컵)</template>
          </p>
        </div>
        <p class="product-price">₩{{ orderFlow.selectedProduct.basePrice.toLocaleString() }}</p>
      </div>
      <p class="product-desc">원하는 맛의 아이스크림을 {{ orderFlow.selectedProduct.productName }}로 즐기세요!</p>

      <p v-if="isCupConeMode && cart.orderType === 'TAKEOUT'" class="warning-text">★★ 콘 제품은 포장이 불가합니다 ★★</p>

      <div v-if="isCupConeMode" class="options">
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'CUP' }"
          @click="orderFlow.containerType = 'CUP'"
        >
          <img :src="containerCup" alt="" class="option-img" />
          <span class="option-label">컵</span>
        </button>
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'CONE' }"
          @click="orderFlow.containerType = 'CONE'"
        >
          <img :src="containerCone" alt="" class="option-img" />
          <span class="option-label">콘</span>
        </button>
        <button
          type="button"
          class="option-card"
          :class="{ selected: orderFlow.containerType === 'WAFFLE_CONE' }"
          @click="orderFlow.containerType = 'WAFFLE_CONE'"
        >
          <img :src="containerWaffleCone" alt="와플콘" class="option-img" />
          <span class="option-label">와플콘(+500원)</span>
        </button>
      </div>

      <!-- 포장(테이크아웃) 대용량 상품: 숟가락 개수 / 드라이아이스 시간 -->
      <div v-else class="spoon-dryice-options">
        <div class="option-group">
          <p class="option-group-title">숟가락 개수</p>
          <div class="stepper">
            <button type="button" @click="orderFlow.spoonCount = Math.max(0, orderFlow.spoonCount - 1)">−</button>
            <span class="stepper-value">{{ orderFlow.spoonCount }}</span>
            <button type="button" :disabled="orderFlow.spoonCount >= 10" @click="orderFlow.spoonCount = Math.min(10, orderFlow.spoonCount + 1)">+</button>
          </div>
        </div>
        <div class="option-group">
          <p class="option-group-title">드라이아이스 시간</p>
          <div class="dryice-choices">
            <button
              v-for="choice in DRY_ICE_CHOICES"
              :key="String(choice.value)"
              type="button"
              class="dryice-chip"
              :class="{ selected: orderFlow.dryIceMinutes === choice.value }"
              @click="orderFlow.dryIceMinutes = choice.value"
            >
              {{ choice.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'product'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>이전</span>
      </button>
      <button type="button" class="confirm-btn" @click="orderFlow.proceedPastContainer">플레이버(맛) 선택</button>
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

const closeXSvg = closeXRaw
const orderFlow = useOrderFlowStore()
const cart = useCartStore()

const isCupConeMode = computed(() => orderFlow.selectedProduct?.containerPolicy === 'CUP_OR_CONE')

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
  margin: 0 auto;
  padding-bottom: 233px;
  background: #fff;
  min-height: 100vh;
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
  padding: 24px 32px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.preview-images {
  position: relative;
  width: 140px;
  height: 130px;
  flex-shrink: 0;
}

.preview-img {
  position: absolute;
  object-fit: contain;
}

.preview-img--cone {
  width: 90px;
  height: 120px;
  left: 0;
  top: 6px;
  transform: rotate(14deg);
}

.preview-img--cup {
  width: 80px;
  height: 88px;
  left: 60px;
  top: 30px;
  transform: rotate(-13deg);
}

.preview-single {
  width: 100px;
  height: 100px;
  object-fit: contain;
  flex-shrink: 0;
}

.product-text {
  flex: 1;
}

.product-name {
  margin: 0;
  font-size: 25px;
  color: #000;
  line-height: 1.3;
}

.product-price {
  margin: 0;
  font-size: 40px;
  color: #f20c93;
  font-weight: 500;
}

.product-desc {
  margin: 16px 0 0;
  font-size: 20px;
  color: #989898;
}

.warning-text {
  margin: 24px 0 0;
  font-size: 20px;
  color: #f20c0c;
}

.options {
  display: flex;
  gap: 24px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.option-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 220px;
  height: 220px;
  border: 3px solid #d2d2d2;
  border-radius: 24px;
  background: #fff;
  cursor: pointer;
}

.option-card.selected {
  border-color: #ef9bcd;
}

.option-img {
  width: 90px;
  height: 90px;
  object-fit: contain;
}

.option-label {
  font-size: 20px;
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
  margin: 0 0 16px;
  font-size: 20px;
  color: #000;
}

.stepper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stepper button {
  width: 48px;
  height: 48px;
  border: 1px solid #d2d2d2;
  border-radius: 50%;
  background: #fff;
  font-size: 20px;
  cursor: pointer;
}

.stepper button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.stepper-value {
  min-width: 40px;
  text-align: center;
  font-size: 25px;
  color: #000;
}

.dryice-choices {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dryice-chip {
  padding: 12px 20px;
  border: 3px solid #d2d2d2;
  border-radius: 24px;
  background: #fff;
  color: #9f9f9f;
  font-size: 18px;
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
