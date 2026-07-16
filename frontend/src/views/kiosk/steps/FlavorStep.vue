<template>
  <!-- 5단계: 맛 선택 (CU-006) -->
  <div class="page">
    <!-- 상품정보/플레이버 탭 -->
    <nav class="tab-bar">
      <button type="button" class="tab" @click="orderFlow.step = 'product'">상품정보</button>
      <button type="button" class="tab active">플레이버</button>
    </nav>

    <div v-if="orderFlow.selectedProduct.requiresFlavorSelection" class="content">
      <p class="progress-text">
        맛 선택 ({{ orderFlow.selectedFlavorIds.length }} / {{ orderFlow.selectedProduct.selectableFlavorCount }})
        <span class="progress-hint">같은 맛을 여러 번 선택할 수 있어요</span>
      </p>

      <ul class="flavor-grid">
        <li v-for="flavor in orderFlow.flavors" :key="flavor.flavorId">
          <button
            type="button"
            class="flavor-card"
            :class="{ selected: orderFlow.flavorSelectedCount(flavor.flavorId) > 0 }"
            :disabled="!orderFlow.canPickMoreFlavor()"
            @click="orderFlow.toggleFlavor(flavor.flavorId)"
          >
            <span class="flavor-thumb">
              <img v-if="flavor.imageUrl" :src="flavor.imageUrl" :alt="flavor.flavorName" />
              <span v-else class="flavor-thumb--empty" />
              <span v-if="orderFlow.flavorSelectedCount(flavor.flavorId) > 0" class="flavor-count-badge">
                {{ orderFlow.flavorSelectedCount(flavor.flavorId) }}
              </span>
            </span>
            <span class="flavor-name">{{ flavor.flavorName }}</span>
          </button>
        </li>
      </ul>
    </div>

    <div v-if="orderFlow.selectedProduct.isLarge" class="large-options">
      <label>
        숟가락 개수
        <input v-model.number="orderFlow.spoonCount" type="number" min="0" />
      </label>
      <label>
        드라이아이스 시간(분)
        <select v-model.number="orderFlow.dryIceMinutes">
          <option :value="null">사용 안함</option>
          <option :value="10">10분</option>
          <option :value="20">20분</option>
          <option :value="30">30분</option>
        </select>
      </label>
    </div>

    <!-- 담은 맛을 화면 하단에 실시간 표시 -->
    <footer v-if="orderFlow.selectedFlavorSummary.length" class="flavor-summary-bar">
      <p class="summary-label">선택한 맛</p>
      <ul class="summary-circles">
        <li v-for="entry in orderFlow.selectedFlavorSummary" :key="entry.flavorId" class="summary-circle-wrap">
          <button type="button" class="summary-circle" @click="orderFlow.removeOneFlavor(entry.flavorId)">
            <img v-if="flavorImage(entry.flavorId)" :src="flavorImage(entry.flavorId)" :alt="entry.flavorName" />
            <span v-if="entry.count > 1" class="summary-count-badge">{{ entry.count }}</span>
          </button>
          <span class="summary-name">{{ entry.flavorName }}</span>
        </li>
        <li v-for="n in emptySlotCount" :key="`empty-${n}`" class="summary-circle-wrap">
          <span class="summary-circle summary-circle--empty" />
        </li>
      </ul>
    </footer>

    <div class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'product'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>이전</span>
      </button>
      <button type="button" class="confirm-btn" :disabled="!orderFlow.canConfirmFlavor" @click="orderFlow.confirmAddToCart">
        {{ orderFlow.editingItemId ? '수정 완료' : '플레이버(맛) 선택' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios.svg'

const orderFlow = useOrderFlowStore()

function flavorImage(flavorId) {
  return orderFlow.flavors.find((f) => f.flavorId === flavorId)?.imageUrl ?? null
}

const emptySlotCount = computed(() => {
  const max = orderFlow.selectedProduct?.selectableFlavorCount ?? 0
  return Math.max(0, max - orderFlow.selectedFlavorIds.length)
})
</script>

<style scoped>
.page {
  max-width: 1024px;
  margin: 0 auto;
  padding-bottom: 233px;
  background: #f1f1f1;
  min-height: 100vh;
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
  background: #fff;
  padding: 24px 16px;
}

.progress-text {
  font-size: 20px;
  color: #333;
  margin: 0 0 16px;
}

.progress-hint {
  margin-left: 8px;
  font-size: 14px;
  color: #989898;
}

.flavor-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.flavor-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 2px solid transparent;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
}

.flavor-card.selected {
  border-color: #f20c93;
}

.flavor-card:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.flavor-thumb {
  position: relative;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flavor-thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.flavor-thumb--empty {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  background: #f4f4f4;
}

.flavor-count-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 22px;
  height: 22px;
  padding: 0 4px;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flavor-name {
  font-size: 17px;
  color: #000;
  text-align: center;
}

.large-options {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #fff;
}

.flavor-summary-bar {
  position: fixed;
  bottom: 233px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  background: #fafafa;
  border-top: 1px solid #ddd;
  padding: 0.75rem 1.5rem;
}

.summary-label {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;
}

.summary-circles {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.summary-circle-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.summary-circle {
  position: relative;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  border: 3px solid #f20c93;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}

.summary-circle img {
  width: 70%;
  height: 70%;
  object-fit: contain;
}

.summary-circle--empty {
  border-color: #d2d2d2;
  cursor: default;
}

.summary-count-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-name {
  font-size: 12px;
  color: #333;
  max-width: 70px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
</style>
