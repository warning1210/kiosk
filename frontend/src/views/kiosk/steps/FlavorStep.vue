<template>
  <!-- 5단계: 맛 선택 (CU-006) -->
  <div class="page">
    <button type="button" class="icon-btn close-btn" aria-label="처음으로" @click="orderFlow.goHome">
      <span v-html="closeXSvg"></span>
    </button>

    <!-- 상품정보/플레이버 탭: 컵/콘을 고르거나(용기 선택 화면을 거친) 테이크아웃 대용량 상품(숟가락/드라이아이스)만 '상품정보'로 되돌아갈 수 있다 -->
    <nav class="tab-bar">
      <button v-if="showProductInfoTab" type="button" class="tab" @click="orderFlow.step = 'container'">상품정보</button>
      <button type="button" class="tab active">플레이버</button>
    </nav>

    <div v-if="orderFlow.selectedProduct.requiresFlavorSelection" class="content">
      <p class="progress-text">
        맛 선택 ({{ orderFlow.selectedFlavorIds.length }} / {{ orderFlow.selectedProduct.selectableFlavorCount }})
        <span class="progress-hint">같은 맛을 여러 번 선택할 수 있어요</span>
      </p>

      <div
        class="flavor-viewport"
        @pointerdown="onSwipeStart"
        @pointermove="onSwipeMove"
        @pointerup="onSwipeEnd"
        @pointercancel="onSwipeEnd"
      >
        <div class="flavor-track" :style="{ transform: `translateX(-${currentPage * 100}%)` }">
          <ul v-for="(page, pageIndex) in flavorPages" :key="pageIndex" class="flavor-grid">
            <li v-for="flavor in page" :key="flavor.flavorId">
              <button
                type="button"
                class="flavor-card"
                :class="{ selected: orderFlow.flavorSelectedCount(flavor.flavorId) > 0 }"
                :disabled="!orderFlow.canPickMoreFlavor() && orderFlow.flavorSelectedCount(flavor.flavorId) === 0"
                @click="onFlavorClick(flavor.flavorId)"
              >
                <span class="flavor-thumb">
                  <img v-if="flavor.imageUrl" :src="flavor.imageUrl" :alt="flavor.flavorName" />
                  <span v-else class="flavor-thumb--empty" />
                  <span v-if="orderFlow.flavorSelectedCount(flavor.flavorId) > 0" class="flavor-count-badge">
                    {{ orderFlow.flavorSelectedCount(flavor.flavorId) }}
                  </span>
                </span>
                <span class="flavor-name">{{ flavor.flavorName }}</span>
                <span v-if="orderFlow.isMonthlyFlavorId(flavor.flavorId)" class="monthly-badge">이달의 맛</span>
              </button>
            </li>
          </ul>
        </div>
      </div>

      <!-- 페이지 인디케이터 -->
      <div v-if="totalPages > 1" class="page-dots">
        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          class="page-dot"
          :class="{ active: currentPage === page - 1 }"
          :aria-label="`${page}페이지`"
          @click="currentPage = page - 1"
        ></button>
      </div>
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
import { computed, ref } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios-pink.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'

const closeXSvg = closeXRaw

const FLAVORS_PER_PAGE = 12 // 4열 x 3행
const SWIPE_THRESHOLD = 40 // px

const orderFlow = useOrderFlowStore()

// 이전 단계(상품정보)가 실제로 존재하는 상품만 탭을 보여준다:
// 컵/콘 선택이 있었던 상품, 또는 테이크아웃 대용량 상품(숟가락/드라이아이스)
const showProductInfoTab = computed(() => {
  const product = orderFlow.selectedProduct
  return product ? orderFlow.needsContainerStep(product) : false
})

function flavorImage(flavorId) {
  return orderFlow.flavors.find((f) => f.flavorId === flavorId)?.imageUrl ?? null
}

const currentPage = ref(0)
const flavorPages = computed(() => {
  const pages = []
  for (let i = 0; i < orderFlow.flavors.length; i += FLAVORS_PER_PAGE) {
    pages.push(orderFlow.flavors.slice(i, i + FLAVORS_PER_PAGE))
  }
  return pages.length ? pages : [[]]
})
const totalPages = computed(() => flavorPages.value.length)

// 마우스 드래그(PC)/터치 스와이프로 페이지 전환 - Pointer Events라 둘 다 같은 핸들러로 처리됨
let swipeStartX = null
const isDragging = ref(false)

function onSwipeStart(e) {
  swipeStartX = e.clientX
}

function onSwipeMove(e) {
  if (swipeStartX === null) return
  // 카드 클릭(탭)이 스와이프로 오인되지 않도록, 실제로 좀 움직였을 때만 드래그 중으로 취급
  isDragging.value = Math.abs(e.clientX - swipeStartX) > 10
}

function onSwipeEnd(e) {
  if (swipeStartX === null) return
  const delta = e.clientX - swipeStartX
  swipeStartX = null
  if (Math.abs(delta) < SWIPE_THRESHOLD) {
    isDragging.value = false
    return
  }
  if (delta < 0 && currentPage.value < totalPages.value - 1) currentPage.value += 1
  if (delta > 0 && currentPage.value > 0) currentPage.value -= 1
  // 드래그로 넘긴 직후 발생하는 click까지 막고 나서 풀어준다
  requestAnimationFrame(() => { isDragging.value = false })
}

async function onFlavorClick(flavorId) {
  if (isDragging.value) return
  if (orderFlow.isMonthlyFlavorId(flavorId) && orderFlow.selectedProduct?.productName === '싱글레귤러') {
    const canSelect = await orderFlow.offerMonthlyFlavorUpgrade()
    if (!canSelect) return
  }
  // 다 채운 상태에서 이미 담은 맛을 다시 누르면, 새로 추가하는 대신 1개 취소한다
  if (!orderFlow.canPickMoreFlavor() && orderFlow.flavorSelectedCount(flavorId) > 0) {
    orderFlow.removeOneFlavor(flavorId)
    return
  }
  orderFlow.toggleFlavor(flavorId)
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
  background: #fff;
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

.flavor-viewport {
  overflow: hidden;
  touch-action: pan-y;
  user-select: none;
  cursor: grab;
}

.flavor-track {
  display: flex;
  transition: transform 0.35s ease;
}

.flavor-grid {
  flex: 0 0 100%;
  min-width: 100%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.page-dots {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.page-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: #d9d9d9;
  padding: 0;
  cursor: pointer;
}

.page-dot.active {
  background: #f20c93;
}

.flavor-card {
  width: 100%;
  min-height: 155px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 4px;
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
  width: 72px;
  height: 72px;
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
  font-size: 14px;
  color: #000;
  text-align: center;
  width: 100%;
  min-height: 34px;
  padding: 0 2px;
  line-height: 1.2;
  white-space: normal;
  overflow: visible;
  word-break: keep-all;
}

.monthly-badge {
  color: #f20c93;
  font-size: 12px;
  font-weight: 700;
}

.flavor-summary-bar {
  position: fixed;
  bottom: 233px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1024px;
  background: #fff;
  border-top: 1px solid #eee;
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
