<template>
  <!-- 1단계: 매장/포장 선택 (CU-002) - 팝업 형태 -->
  <div class="modal-backdrop">
    <div class="modal">
      <button type="button" class="close-btn" aria-label="처음으로" @click="orderFlow.goHome">
        <span v-html="closeXSvg"></span>
      </button>
      <img class="logo" :src="logo" alt="배스킨라빈스" />
      <h2>주문 방법을<br />선택해주세요</h2>
      <p class="subtitle">Please select your order method.</p>

      <div class="options">
        <button type="button" class="option" @click="orderFlow.selectOrderType('TAKEOUT')">
          <span class="icon-circle">
            <img :src="takeoutBag" alt="" />
          </span>
          <span class="option-label">가져가기</span>
          <span class="option-sub">Take Out</span>
        </button>
        <button type="button" class="option" @click="orderFlow.selectOrderType('DINE_IN')">
          <span class="icon-circle">
            <img :src="dineinCup" alt="" />
          </span>
          <span class="option-label">먹고가기</span>
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

const closeXSvg = closeXRaw
const orderFlow = useOrderFlowStore()
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
  width: min(510px, 90vw);
  padding: 48px 40px;
  background: #fff;
  border-radius: 26px;
  text-align: center;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
}

.close-btn :deep(svg) {
  width: 44px;
  height: 44px;
}

.logo {
  width: 96px;
  height: 88px;
  object-fit: contain;
  margin-bottom: 24px;
}

.modal h2 {
  margin: 0;
  font-size: 40px;
  font-weight: 500;
  line-height: 1.2;
  color: #000;
}

.subtitle {
  margin: 12px 0 32px;
  font-size: 20px;
  color: #acacac;
}

.options {
  display: flex;
  gap: 32px;
}

.option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 156px;
  padding: 20px 0;
  border: 3px solid #d2d2d2;
  border-radius: 25px;
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
  width: 64px;
  height: 64px;
}

.icon-circle img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.option-label {
  font-size: 20px;
  color: #f20c93;
}

.option-sub {
  font-size: 13px;
  color: #acacac;
}
</style>
