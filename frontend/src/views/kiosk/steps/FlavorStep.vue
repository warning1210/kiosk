<template>
  <!-- 5단계: 맛 선택 (CU-006) -->
  <!-- 상품 화면과 동일하게 현재 화면에 클래스를 직접 붙여 쉬운모드 2열 스타일을 적용한다. -->
  <div class="page" :class="{ 'easy-mode-page': orderFlow.easyMode }">
    <button type="button" class="icon-btn close-btn" :aria-label="t('goHome')" @click="orderFlow.goHome">
      <span v-html="closeXSvg"></span>
    </button>

    <!-- 상품정보/플레이버 탭: 컵/콘을 고르거나(용기 선택 화면을 거친) 테이크아웃 대용량 상품(숟가락/드라이아이스)만 '상품정보'로 되돌아갈 수 있다 -->
    <nav class="tab-bar">
      <button v-if="showProductInfoTab" type="button" class="tab" @click="orderFlow.step = 'container'">{{ t('productInfo') }}</button>
      <button type="button" class="tab active">{{ t('flavor') }}</button>
    </nav>

    <div v-if="orderFlow.selectedProduct.requiresFlavorSelection" class="content">
      <p class="progress-text">
        {{ t('flavorSelection') }} ({{ orderFlow.selectedFlavorIds.length }} / {{ orderFlow.selectedProduct.selectableFlavorCount }})
        <span class="progress-hint">{{ t('sameFlavorHint') }}</span>
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
                @click="onFlavorClick(flavor)"
              >
                <span class="flavor-thumb">
                  <img v-if="flavor.imageUrl" :src="flavor.imageUrl" :alt="flavorName(flavor.flavorName)" />
                  <span v-else class="flavor-thumb--empty" />
                  <span v-if="orderFlow.flavorSelectedCount(flavor.flavorId) > 0" class="flavor-count-badge">
                    {{ orderFlow.flavorSelectedCount(flavor.flavorId) }}
                  </span>
                </span>
                <span class="flavor-name">{{ flavorName(flavor.flavorName) }}</span>
                <span v-if="orderFlow.isMonthlyFlavorId(flavor.flavorId)" class="monthly-badge">
                  {{ t('monthlyFlavor') }} · {{ flavor.sizeUpToProductName ? `${menuName(flavor.sizeUpToProductName)} ${t('sizeUp')}` : t('sizeUpAvailable') }}
                </span>
                <!-- 이달의 맛은 500원 사이즈업 전용이므로 기존 DB의 600원 할인값/배지는 화면에 노출하지 않는다. -->
                <span v-if="flavor.discountType && !orderFlow.isMonthlyFlavorId(flavor.flavorId)" class="discount-badge">{{ discountLabel(flavor) }}</span>
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

    <!-- EX 프로젝트처럼 같은 맛 선택 화면에서 선택한 맛 설명을 요약바 바로 위에 표시 -->
    <aside
      v-if="selectedDescriptionTitle"
      class="flavor-description"
      :class="{
        'without-summary': !orderFlow.selectedFlavorSummary.length,
        'without-image': !selectedDescriptionFlavor?.imageUrl
      }"
    >
      <img v-if="selectedDescriptionFlavor?.imageUrl" :src="selectedDescriptionFlavor.imageUrl" :alt="selectedDescriptionTitle" />
      <div>
        <strong>{{ selectedDescriptionTitle }}</strong>
        <p>{{ selectedDescription || t('defaultFlavorDescription') }}</p>
        <small v-if="selectedDescriptionFlavor?.allergyInfo">{{ t('allergy') }} · {{ selectedDescriptionFlavor.allergyInfo }}</small>
      </div>
    </aside>

    <!-- 담은 맛을 화면 하단에 실시간 표시 -->
    <footer v-if="orderFlow.selectedFlavorSummary.length" class="flavor-summary-bar">
      <p class="summary-label">{{ t('selectedFlavors') }}</p>
      <ul class="summary-circles">
        <li v-for="entry in orderFlow.selectedFlavorSummary" :key="entry.flavorId" class="summary-circle-wrap">
          <button type="button" class="summary-circle" @click="orderFlow.removeOneFlavor(entry.flavorId)">
            <img v-if="flavorImage(entry.flavorId)" :src="flavorImage(entry.flavorId)" :alt="flavorName(entry.flavorName)" />
            <span v-if="entry.count > 1" class="summary-count-badge">{{ entry.count }}</span>
          </button>
          <span class="summary-name">{{ flavorName(entry.flavorName) }}</span>
        </li>
        <li v-for="n in emptySlotCount" :key="`empty-${n}`" class="summary-circle-wrap">
          <span class="summary-circle summary-circle--empty" />
        </li>
      </ul>
    </footer>

    <div class="bottom-bar">
      <button type="button" class="prev-btn" @click="orderFlow.step = 'product'">
        <img :src="arrowForwardIos" alt="" class="prev-arrow" />
        <span>{{ t('previous') }}</span>
      </button>
      <button type="button" class="confirm-btn" :disabled="!orderFlow.canConfirmFlavor" @click="orderFlow.confirmAddToCart">
        {{ orderFlow.editingItemId ? t('editComplete') : orderFlow.selectedProduct.requiresFlavorSelection ? t('selectFlavor') : t('addToCart') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useOrderFlowStore } from '../../../stores/orderFlow'
import arrowForwardIos from '../../../assets/kiosk/icons/arrow-forward-ios-pink.svg'
import closeXRaw from '../../../assets/kiosk/icons/close-x.svg?raw'
import { useKioskI18n } from '../../../composables/useKioskI18n'

const closeXSvg = closeXRaw

// 일반모드는 4열×3행, 쉬운모드는 큰 맛 카드 2열×3행으로 페이지를 나눈다.
const flavorsPerPage = computed(() => orderFlow.easyMode ? 6 : 12)
const SWIPE_THRESHOLD = 40 // px

const orderFlow = useOrderFlowStore()
// 언어 선택값은 단계가 바뀌어도 이 공통 composable을 통해 유지됩니다.
const { t, flavorName, menuName } = useKioskI18n()

function discountLabel(flavor) {
  return flavor.discountType === 'DISCOUNT_RATE'
    ? `${flavor.discountRate}% 할인`
    : `${(flavor.discountAmount ?? 0).toLocaleString()}원 할인`
}
// 로컬 ref로 "마지막으로 누른 맛"을 따로 들고 있으면 화면을 벗어났다 돌아올 때(리마운트) 초기화되거나,
// 요약바에서 맛을 제거해도 갱신되지 않아 설명이 그대로 남는 문제가 생긴다. 대신 실제 선택 상태
// (selectedFlavorIds, 리마운트에도 유지되는 스토어 값)에서 가장 최근에 담긴 맛을 그대로 파생시킨다.
const selectedDescriptionFlavor = computed(() => {
  const lastFlavorId = orderFlow.selectedFlavorIds[orderFlow.selectedFlavorIds.length - 1]
  if (lastFlavorId == null) return null
  return orderFlow.flavors.find((f) => f.flavorId === lastFlavorId) ?? null
})
const selectedDescription = computed(() => {
  if (!orderFlow.selectedProduct?.requiresFlavorSelection) return orderFlow.selectedProduct?.description?.trim() ?? ''
  return selectedDescriptionFlavor.value?.description?.trim() ?? ''
})
const selectedDescriptionTitle = computed(() =>
  orderFlow.selectedProduct?.requiresFlavorSelection
    ? flavorName(selectedDescriptionFlavor.value?.flavorName ?? '')
    : orderFlow.selectedProduct?.productName ?? ''
)

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
  for (let i = 0; i < orderFlow.flavors.length; i += flavorsPerPage.value) {
    pages.push(orderFlow.flavors.slice(i, i + flavorsPerPage.value))
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

async function onFlavorClick(flavor) {
  if (isDragging.value) return
  const flavorId = flavor.flavorId
  if (orderFlow.isMonthlyFlavorId(flavorId)) {
    await orderFlow.offerSizeUp(flavor)
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
  width: 100%;
  margin: 0 auto;
  padding-bottom: 233px;
  background: #fff;
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
  background: #fff;
  padding: 28px 20px 8px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.progress-text {
  font-size: 26px;
  color: #333;
  margin: 0 0 20px;
}

.progress-hint {
  margin-left: 10px;
  font-size: 18px;
  color: #989898;
}

.flavor-viewport {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  touch-action: pan-y;
  user-select: none;
  cursor: grab;
}

.flavor-track {
  display: flex;
  height: 100%;
  transition: transform 0.35s ease;
}

.flavor-grid {
  flex: 0 0 100%;
  min-width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-content: start;
  row-gap: 26px;
  column-gap: 8px;
  list-style: none;
  margin: 0;
  padding: 8px 0 0;
}

/* 쉬운모드에서는 맛도 한 줄에 두 개만 보여주고 사진·이름·행사 배지를 크게 표시한다. */
.easy-mode-page .flavor-grid {
  grid-template-columns: repeat(2, 1fr);
  row-gap: 32px;
  column-gap: 20px;
  padding: 12px 14px 0;
}

.easy-mode-page .flavor-card {
  min-height: 300px;
  padding: 18px 12px;
  border-radius: 24px;
}

.easy-mode-page .flavor-thumb {
  width: 190px;
  height: 190px;
}

.easy-mode-page .flavor-name {
  font-size: 30px;
  font-weight: 700;
}

.easy-mode-page .monthly-badge,
.easy-mode-page .discount-badge {
  padding: 8px 12px;
  font-size: 20px;
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
  min-height: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 14px 6px;
  border: 2px solid transparent;
  border-radius: 18px;
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
  width: 128px;
  height: 128px;
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
  font-size: 22px;
  color: #000;
  text-align: center;
  width: 100%;
  padding: 0 2px;
  line-height: 1.25;
  white-space: normal;
  overflow: visible;
  word-break: keep-all;
}

.monthly-badge {
  color: #f20c93;
  font-size: 15px;
  font-weight: 700;
}

.discount-badge {
  padding: 3px 10px;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 14px;
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
  padding: 1.1rem 1.75rem;
}

.flavor-description {
  position: fixed;
  bottom: 430px;
  left: 50%;
  z-index: 3;
  display: grid;
  grid-template-columns: 92px 1fr;
  align-items: center;
  gap: 18px;
  width: calc(100% - 32px);
  max-width: 992px;
  padding: 18px 24px;
  transform: translateX(-50%);
  border: 1px solid #f0b8d5;
  border-radius: 14px;
  background: #fff5fa;
  box-shadow: 0 8px 22px rgb(94 50 69 / 10%);
}

.flavor-description.without-summary { bottom: 253px; }
.flavor-description.without-image { grid-template-columns: minmax(0, 1fr); }
.flavor-description img { width: 88px; height: 88px; object-fit: contain; }
.flavor-description div { min-width: 0; }
.flavor-description strong { display: block; overflow: hidden; color: #f20c93; font-size: 24px; text-overflow: ellipsis; white-space: nowrap; }
.flavor-description p { overflow: hidden; margin: 8px 0; color: #5f5057; font-size: 19px; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.flavor-description small { color: #9a7e8a; font-size: 16px; }

/* 쉬운모드의 선택 맛 설명은 목록 카드보다 보조 정보이므로 과도하게 키우지 않고 약 15~20%만 확대한다.
   글자만 커져 이미지와 균형이 깨지지 않도록 카드 여백·열 너비·썸네일도 같은 비율로 소폭 늘린다. */
.easy-mode-page .flavor-description {
  grid-template-columns: 110px 1fr;
  gap: 22px;
  padding: 22px 28px;
}

.easy-mode-page .flavor-description.without-image {
  grid-template-columns: minmax(0, 1fr);
}

.easy-mode-page .flavor-description img {
  width: 104px;
  height: 104px;
}

.easy-mode-page .flavor-description strong {
  font-size: 29px;
}

.easy-mode-page .flavor-description p {
  margin: 9px 0;
  font-size: 23px;
}

.easy-mode-page .flavor-description small {
  font-size: 19px;
}

.summary-label {
  margin: 0 0 12px;
  font-size: 20px;
  color: #666;
}

.summary-circles {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
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
  width: 92px;
  height: 92px;
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
  top: -6px;
  right: -6px;
  min-width: 27px;
  height: 27px;
  padding: 0 6px;
  border-radius: 999px;
  background: #f20c93;
  color: #fff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-name {
  font-size: 18px;
  color: #333;
  max-width: 104px;
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
