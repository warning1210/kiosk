<template>
  <!-- 1단계: 매장/포장 선택 (CU-002) - 팝업 형태 -->
  <div class="modal-backdrop">
    <div class="modal">
      <button type="button" class="close-btn" :aria-label="t('goHome')" @click="orderFlow.goHome">
        <span v-html="closeXSvg"></span>
      </button>
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <h2 class="multiline-title">{{ t('orderMethodTitle') }}</h2>
      <p class="subtitle">{{ t('orderMethodSubtitle') }}</p>

      <div class="options">
        <button type="button" class="option" @click="orderFlow.selectOrderType('TAKEOUT')">
          <span class="icon-circle">
            <img :src="takeoutBag" alt="" />
          </span>
          <span class="option-label">{{ t('takeout') }}</span>
          <span class="option-sub">Take Out</span>
        </button>
        <button type="button" class="option" @click="orderFlow.selectOrderType('DINE_IN')">
          <span class="icon-circle">
            <img :src="dineinCup" alt="" />
          </span>
          <span class="option-label">{{ t('dineIn') }}</span>
          <span class="option-sub">Dine In</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import logo from '../../../assets/kiosk/logo.png'
import takeoutBag from '../../../assets/kiosk/icons/takeout-bag.png'
import dineinCup from '../../../assets/kiosk/icons/dinein-cup.png'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import { useKioskI18n } from '../../../composables/useKioskI18n'

const closeXSvg = closeXRaw
const orderFlow = useOrderFlowStore()
// 상품 화면에서 선택한 언어를 주문 방법 화면에서도 공유합니다.
const { t } = useKioskI18n()
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(720px, 92vw);
  padding: 64px 56px;
  background: #fff;
  border-radius: 32px;
  text-align: center;
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.close-btn :deep(svg) {
  width: 56px;
  height: 56px;
}

.logo {
  width: 132px;
  height: 120px;
  object-fit: contain;
  margin-bottom: 30px;
}

.modal h2 {
  margin: 0;
  font-size: 54px;
  font-weight: 500;
  line-height: 1.2;
  color: #000;
}

.multiline-title {
  white-space: pre-line;
}

.subtitle {
  margin: 16px 0 44px;
  font-size: 26px;
  color: #acacac;
}

.options {
  display: flex;
  gap: 44px;
}

.option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 230px;
  padding: 34px 0;
  border: 3px solid #d2d2d2;
  border-radius: 30px;
  background: #fff;
  cursor: pointer;
}

.option:hover {
  border-color: #f20c93;
}

.icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104px;
  height: 104px;
}

.icon-circle img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.option-label {
  font-size: 30px;
  color: #f20c93;
}

.option-sub {
  font-size: 17px;
  color: #acacac;
}
</style>
