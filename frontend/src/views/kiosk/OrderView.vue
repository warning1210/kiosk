<template>
  <main class="kiosk-page">
    <header class="topbar">
      <RouterLink :to="{ name: 'kiosk-size', query: { orderType } }" class="back">‹</RouterLink>
      <div>
        <p class="eyebrow">BASKIN ROBBINS</p>
        <h1>{{ productName }} · 맛 {{ maxSelection }}개를 선택해 주세요</h1>
      </div>
      <span class="count">{{ selected.length }} / {{ maxSelection }}</span>
    </header>

    <section class="content">
      <p v-if="loading" class="state">아이스크림을 불러오는 중입니다.</p>
      <div v-else-if="error" class="state error">
        <strong>아이스크림을 불러오지 못했습니다.</strong>
        <span>백엔드 서버와 /api/flavors 연결을 확인해 주세요.</span>
        <button type="button" @click="loadFlavors">다시 시도</button>
      </div>
      <p v-else-if="flavors.length === 0" class="state">판매 중인 아이스크림이 없습니다.</p>

      <template v-else>
      <section class="flavor-section monthly-section">
        <div class="section-title"><span>MONTHLY FLAVOR</span><h2>이달의 맛</h2></div>
        <div class="flavor-grid featured-grid">
        <button
          v-for="flavor in monthlyFlavors"
          :key="flavor.flavorId"
          class="flavor-card monthly-card"
          :class="{ selected: isSelected(flavor.flavorId) }"
          type="button"
          @mouseenter="focusedFlavor = flavor"
          @focus="focusedFlavor = flavor"
          @click="toggleFlavor(flavor)"
        >
          <span class="monthly-badge">이달의 맛</span>
          <span class="image-wrap">
            <img :src="flavor.imageUrl" :alt="flavor.flavorName" loading="lazy">
            <span v-if="isSelected(flavor.flavorId)" class="check">✓</span>
          </span>
          <strong>{{ flavor.flavorName }}</strong>
        </button>
      </div>
      </section>
      <section class="flavor-section">
        <div class="section-title"><span>ALL FLAVORS</span><h2>아이스크림 맛</h2></div>
        <div class="flavor-grid">
          <button
            v-for="flavor in regularFlavors"
            :key="flavor.flavorId"
            class="flavor-card"
            :class="{ selected: isSelected(flavor.flavorId) }"
            type="button"
            @mouseenter="focusedFlavor = flavor"
            @focus="focusedFlavor = flavor"
            @click="toggleFlavor(flavor)"
          >
            <span v-if="flavor.popularityRank" class="rank">{{ flavor.popularityRank }}위</span>
            <span class="image-wrap">
              <img :src="flavor.imageUrl" :alt="flavor.flavorName" loading="lazy">
              <span v-if="isSelected(flavor.flavorId)" class="check">✓</span>
            </span>
            <strong>{{ flavor.flavorName }}</strong>
          </button>
        </div>
      </section>
      </template>
    </section>

    <aside v-if="focusedFlavor" class="flavor-description">
      <img :src="focusedFlavor.imageUrl" :alt="focusedFlavor.flavorName">
      <div>
        <strong>{{ focusedFlavor.flavorName }}</strong>
        <p>{{ focusedFlavor.description || '부드럽고 달콤한 배스킨라빈스 아이스크림입니다.' }}</p>
        <small v-if="focusedFlavor.allergyInfo">알레르기 성분 · {{ focusedFlavor.allergyInfo }}</small>
      </div>
    </aside>

    <footer class="selection-panel">
      <p v-if="monthlyRequired" class="promotion-note" :class="{ complete: hasMonthlyFlavor }">
        {{ hasMonthlyFlavor ? '이달의 맛이 포함되었습니다.' : '업그레이드 상품은 이달의 맛을 1개 이상 선택해 주세요.' }}
      </p>
      <div class="selection-list">
        <button
          v-for="flavor in selected"
          :key="flavor.flavorId"
          class="selected-item"
          type="button"
          @click="removeFlavor(flavor.flavorId)"
        >
          <span class="remove">×</span>
          <img :src="flavor.imageUrl" :alt="flavor.flavorName">
          <span>{{ flavor.flavorName }}</span>
        </button>
        <div v-for="slot in emptySlots" :key="`empty-${slot}`" class="empty-slot">?</div>
      </div>
      <button class="next-button" type="button" :disabled="!canComplete" @click="completeSelection">
        선택 완료 <span>{{ selected.length }}</span>
      </button>
    </footer>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import { useCartStore } from '../../stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const flavors = ref([])
const selected = ref([])
const focusedFlavor = ref(null)
const loading = ref(true)
const error = ref(false)
const orderType = computed(() => route.query.orderType === 'TAKEOUT' ? 'TAKEOUT' : 'DINE_IN')
const productName = computed(() => route.query.productName || '아이스크림')
const maxSelection = computed(() => {
  const value = Number(route.query.flavorCount)
  return Number.isInteger(value) && value >= 1 && value <= 6 ? value : 1
})
const monthlyRequired = computed(() => route.query.monthlyRequired === '1')
const monthlyFlavors = computed(() => flavors.value.filter((flavor) => flavor.monthlyFlavor))
const regularFlavors = computed(() => flavors.value.filter((flavor) => !flavor.monthlyFlavor))
const hasMonthlyFlavor = computed(() => selected.value.some((flavor) => flavor.monthlyFlavor))
const canComplete = computed(() => selected.value.length === maxSelection.value && (!monthlyRequired.value || hasMonthlyFlavor.value))

const emptySlots = computed(() => Math.max(0, maxSelection.value - selected.value.length))

function isSelected(flavorId) {
  return selected.value.some((item) => item.flavorId === flavorId)
}

function toggleFlavor(flavor) {
  focusedFlavor.value = flavor
  if (isSelected(flavor.flavorId)) {
    removeFlavor(flavor.flavorId)
    return
  }
  if (selected.value.length < maxSelection.value) selected.value.push(flavor)
}

function removeFlavor(flavorId) {
  selected.value = selected.value.filter((item) => item.flavorId !== flavorId)
}

function completeSelection() {
  if (!canComplete.value) return
  cart.addItem({
    orderType: orderType.value,
    productId: Number(route.query.productId),
    productName: productName.value,
    basePrice: Number(route.query.basePrice) || 0,
    flavorCount: maxSelection.value,
    flavors: selected.value.map((flavor) => ({
      flavorId: flavor.flavorId,
      flavorName: flavor.flavorName,
      imageUrl: flavor.imageUrl
    }))
  })
  router.push({ name: 'kiosk-size', query: { orderType: orderType.value } })
}

async function loadFlavors() {
  loading.value = true
  error.value = false
  try {
    const { data } = await http.get('/flavors')
    flavors.value = data
  } catch (requestError) {
    console.error(requestError)
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadFlavors)
</script>

<style scoped>
.kiosk-page {
  --pink: #ef3f91;
  min-height: 100vh;
  padding-bottom: 250px;
  color: #372d32;
  background: linear-gradient(180deg, #fff 0%, #fff8fb 100%);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: grid;
  grid-template-columns: 60px 1fr 80px;
  align-items: center;
  padding: 18px clamp(18px, 4vw, 54px);
  background: rgb(255 255 255 / 94%);
  border-bottom: 1px solid #f2e4eb;
  backdrop-filter: blur(12px);
}

.back { color: var(--pink); font-size: 44px; text-decoration: none; }
.eyebrow { margin: 0 0 3px; color: var(--pink); font-size: 11px; font-weight: 800; letter-spacing: .16em; }
h1 { margin: 0; font-size: clamp(22px, 3vw, 34px); }
.count { justify-self: end; color: var(--pink); font-size: 18px; font-weight: 800; }
.content { width: min(960px, 100%); margin: 0 auto; padding: 30px 22px; }
.flavor-section + .flavor-section { margin-top: 42px; padding-top: 30px; border-top: 1px solid #f0e1e8; }
.section-title { margin: 0 0 20px; text-align: center; }
.section-title span { color: var(--pink); font-size: 11px; font-weight: 900; letter-spacing: .14em; }
.section-title h2 { margin: 4px; font-size: 25px; }
.featured-grid { grid-template-columns: repeat(2, minmax(0, 190px)); justify-content: center; }
.monthly-card { padding-top: 18px; }
.monthly-badge { position: absolute; z-index: 2; top: 0; left: 50%; padding: 5px 12px; color: #fff; background: var(--pink); border-radius: 999px; font-size: 12px; font-weight: 900; transform: translateX(-50%); white-space: nowrap; }

.flavor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 26px 16px;
}

.flavor-card {
  position: relative;
  min-width: 0;
  padding: 4px;
  border: 0;
  color: inherit;
  background: transparent;
  cursor: pointer;
}

.image-wrap {
  position: relative;
  display: grid;
  width: min(150px, 100%);
  aspect-ratio: 1;
  margin: auto;
  place-items: center;
  border: 3px solid transparent;
  border-radius: 50%;
  background: #fff;
  transition: .18s ease;
}

.flavor-card img { width: 94%; height: 94%; object-fit: contain; }
.flavor-card strong { display: block; margin-top: 9px; font-size: clamp(12px, 1.8vw, 16px); word-break: keep-all; }
.flavor-card.selected .image-wrap { border-color: var(--pink); box-shadow: 0 8px 20px rgb(239 63 145 / 22%); transform: translateY(-3px); }
.rank { position: absolute; z-index: 2; top: 0; left: 10%; padding: 4px 8px; color: #fff; background: #b37a35; border-radius: 5px; font-size: 12px; font-weight: 800; }
.check { position: absolute; right: 2px; bottom: 4px; display: grid; width: 28px; height: 28px; place-items: center; color: #fff; background: var(--pink); border-radius: 50%; }

.state { display: grid; min-height: 45vh; place-items: center; text-align: center; }
.state.error { align-content: center; gap: 12px; color: #7d5968; }
.state.error span { display: block; }
.state button { padding: 10px 18px; border: 0; border-radius: 999px; color: #fff; background: var(--pink); cursor: pointer; }

.flavor-description {
  position: fixed;
  z-index: 5;
  right: max(20px, calc((100vw - 960px) / 2));
  bottom: 116px;
  left: max(20px, calc((100vw - 960px) / 2));
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 16px;
  align-items: center;
  padding: 14px 18px;
  background: rgb(255 255 255 / 97%);
  border: 1px solid #f0dce6;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgb(94 50 69 / 14%);
  backdrop-filter: blur(14px);
}
.flavor-description img { width: 68px; height: 68px; object-fit: contain; }
.flavor-description strong { color: var(--pink); font-size: 18px; }
.flavor-description p { margin: 5px 0; color: #5f5057; font-size: 14px; }
.flavor-description small { color: #9a7e8a; }

.selection-panel {
  position: fixed;
  z-index: 6;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 14px max(20px, calc((100vw - 960px) / 2));
  background: rgb(255 255 255 / 96%);
  border-top: 1px solid #eadbe2;
  box-shadow: 0 -10px 30px rgb(94 50 69 / 10%);
  backdrop-filter: blur(14px);
}
.promotion-note { position: absolute; top: -39px; right: 0; left: 0; margin: 0; padding: 9px; text-align: center; color: #b12966; background: #fff0f7; border-top: 1px solid #f0c5d9; font-size: 13px; font-weight: 800; }
.promotion-note.complete { color: #287f56; background: #edfff6; border-color: #bcebd4; }

.selection-list { display: flex; flex: 1; gap: 14px; min-width: 0; }
.selected-item, .empty-slot { position: relative; display: grid; width: 88px; min-width: 72px; justify-items: center; border: 0; background: transparent; font-size: 11px; }
.selected-item img, .empty-slot { width: 66px; height: 66px; object-fit: contain; border-radius: 50%; }
.selected-item { cursor: pointer; }
.selected-item span:last-child { overflow: hidden; width: 100%; margin-top: 3px; text-overflow: ellipsis; white-space: nowrap; }
.remove { position: absolute; z-index: 1; top: -3px; right: 4px; display: grid; width: 20px; height: 20px; place-items: center; color: #fff; background: #62585d; border-radius: 50%; font-size: 16px; }
.empty-slot { place-items: center; color: var(--pink); border: 2px dashed #efb4d0; background: #fff8fb; font-size: 28px; font-weight: 300; }
.next-button { min-width: 150px; padding: 18px 20px; border: 0; border-radius: 16px; color: #fff; background: var(--pink); font-size: 17px; font-weight: 800; cursor: pointer; }
.next-button:disabled { background: #d8cbd1; cursor: not-allowed; }
.next-button span { display: inline-grid; width: 26px; height: 26px; margin-left: 8px; place-items: center; color: var(--pink); background: #fff; border-radius: 50%; }

@media (max-width: 640px) {
  .flavor-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px 8px; }
  .selection-panel { gap: 8px; padding: 10px; }
  .selection-list { gap: 3px; overflow-x: auto; }
  .selected-item, .empty-slot { width: 62px; min-width: 62px; }
  .selected-item img, .empty-slot { width: 52px; height: 52px; }
  .next-button { min-width: 116px; padding: 15px 10px; font-size: 14px; }
  .flavor-description { right: 10px; bottom: 94px; left: 10px; grid-template-columns: 54px 1fr; padding: 10px; }
  .flavor-description img { width: 52px; height: 52px; }
  .flavor-description p { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
}
</style>
