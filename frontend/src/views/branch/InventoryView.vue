<template>
  <section>
    <header class="page-header">
      <div>
        <h2>재고 현황</h2>
        <p>재고가 적은 순으로 정렬 · 안전재고 이하 자동 알림</p>
      </div>
      <button type="button" class="primary" @click="openModal(null)">+ 재고 신청</button>
    </header>

    <div class="toolbar">
      <input v-model="keyword" type="text" placeholder="상품명 검색..." @input="load" />
    </div>

    <p v-if="soldOutCount > 0" class="banner danger">
      품절 {{ soldOutCount }}건 — 키오스크에서 자동 제외됨
    </p>
    <p v-if="lowCount > 0" class="banner warning">
      안전재고 이하 {{ lowCount }}건 — 재고 신청을 권장합니다
    </p>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="banner danger">{{ error }}</p>

    <table v-else class="inventory-table">
      <thead>
        <tr>
          <th>상품명</th>
          <th>카테고리</th>
          <th>현재고/안전</th>
          <th>상태</th>
          <th>키오스크</th>
          <th>관리</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.branchInventoryId">
          <td>{{ item.flavorName }}</td>
          <td>{{ item.categoryName ?? '-' }}</td>
          <td>{{ item.currentQuantity }} / {{ item.safetyQuantity }}</td>
          <td>
            <span class="badge" :class="statusClass(item.inventoryStatus)">{{ statusLabel(item.inventoryStatus) }}</span>
          </td>
          <td>{{ item.isKioskVisible ? '노출' : '숨김' }}</td>
          <td>
            <button type="button" class="link" @click="openModal(item.flavorId)">신청</button>
          </td>
        </tr>
        <tr v-if="items.length === 0">
          <td colspan="6" class="empty">등록된 재고가 없습니다</td>
        </tr>
      </tbody>
    </table>

    <StockRequestFormModal
      v-if="modalOpen"
      :inventory-items="items"
      :preset-flavor-id="presetFlavorId"
      @close="modalOpen = false"
      @submitted="onSubmitted"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchBranchInventory } from '../../api/branchInventory'
import StockRequestFormModal from './StockRequestFormModal.vue'

const items = ref([])
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const modalOpen = ref(false)
const presetFlavorId = ref(null)

const soldOutCount = computed(() => items.value.filter((i) => i.inventoryStatus === 'SOLD_OUT').length)
const lowCount = computed(() => items.value.filter((i) => i.inventoryStatus === 'LOW').length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchBranchInventory({ keyword: keyword.value || undefined })
  } catch (e) {
    error.value = e.response?.data?.error?.message ?? '재고 정보를 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

function statusLabel(status) {
  if (status === 'SOLD_OUT') return '품절'
  if (status === 'LOW') return '부족'
  return '정상'
}

function statusClass(status) {
  if (status === 'SOLD_OUT') return 'danger'
  if (status === 'LOW') return 'warning'
  return 'success'
}

function openModal(flavorId) {
  presetFlavorId.value = flavorId
  modalOpen.value = true
}

function onSubmitted() {
  modalOpen.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.page-header h2 {
  margin: 0 0 0.25rem;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  font-size: 0.8125rem;
}

.primary {
  border: none;
  background: #4f46e5;
  color: white;
  border-radius: 8px;
  padding: 0.625rem 1rem;
  cursor: pointer;
  height: fit-content;
}

.toolbar {
  margin-bottom: 0.75rem;
}

.toolbar input {
  width: 260px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.banner {
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  margin-bottom: 0.5rem;
}

.banner.danger {
  background: #fef2f2;
  color: #b91c1c;
}

.banner.warning {
  background: #fffbeb;
  color: #b45309;
}

.inventory-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.inventory-table th,
.inventory-table td {
  text-align: left;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid #f1f1f4;
  font-size: 0.875rem;
}

.badge {
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
}

.badge.success {
  background: #ecfdf5;
  color: #059669;
}

.badge.warning {
  background: #fffbeb;
  color: #b45309;
}

.badge.danger {
  background: #fef2f2;
  color: #b91c1c;
}

.link {
  border: none;
  background: transparent;
  color: #4f46e5;
  cursor: pointer;
  font-size: 0.8125rem;
}

.empty {
  text-align: center;
  color: #9ca3af;
}
</style>
