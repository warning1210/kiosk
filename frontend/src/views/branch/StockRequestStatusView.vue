<template>
  <section>
    <header class="page-header">
      <h2>입고·신청 현황</h2>
      <p>신청한 재고의 배송 상태 · 배송 완료 시 재고에 자동 반영</p>
    </header>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }} {{ countFor(tab.key) }}
      </button>
    </div>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="banner danger">{{ error }}</p>

    <div v-else class="cards">
      <article v-for="request in filteredRequests" :key="request.stockRequestId" class="card">
        <div class="summary">
          <strong>{{ itemSummary(request) }}</strong>
          <span>{{ formatDate(request.requestedAt) }} 신청 · {{ totalQuantity(request) }}개</span>
        </div>

        <ol class="timeline" :data-step="stepIndex(request.requestStatus)">
          <li>신청</li>
          <li>배송중</li>
          <li>수령확인</li>
        </ol>

        <p v-if="request.requestStatus === 'REJECTED'" class="rejection">반려: {{ request.rejectionReason }}</p>

        <div class="actions">
          <button
            v-if="request.requestStatus === 'PENDING'"
            type="button"
            class="ghost"
            :disabled="actingId === request.stockRequestId"
            @click="onCancel(request)"
          >
            신청 취소
          </button>
          <button
            v-else-if="request.requestStatus === 'SHIPPING'"
            type="button"
            class="primary"
            :disabled="actingId === request.stockRequestId"
            @click="onConfirmReceipt(request)"
          >
            배송완료 확인
          </button>
          <button v-else-if="request.requestStatus === 'DELIVERED'" type="button" class="ghost" disabled>
            재고 반영됨
          </button>
        </div>
      </article>

      <p v-if="filteredRequests.length === 0" class="empty">해당 상태의 신청 건이 없습니다</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { cancelStockRequest, confirmReceipt, fetchBranchStockRequests } from '../../api/branchStockRequest'

const tabs = [
  { key: 'ALL', label: '전체' },
  { key: 'REQUESTED', label: '신청됨' },
  { key: 'SHIPPING', label: '배송중' },
  { key: 'DELIVERED', label: '완료' }
]

const requests = ref([])
const loading = ref(true)
const error = ref('')
const activeTab = ref('ALL')
const actingId = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchBranchStockRequests({ size: 100 })
    requests.value = page.content
  } catch (e) {
    error.value = e.response?.data?.error?.message ?? '신청 현황을 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

function matchesTab(request, tabKey) {
  if (tabKey === 'ALL') return true
  if (tabKey === 'REQUESTED') return request.requestStatus === 'PENDING' || request.requestStatus === 'PREPARING'
  if (tabKey === 'SHIPPING') return request.requestStatus === 'SHIPPING'
  if (tabKey === 'DELIVERED') return request.requestStatus === 'DELIVERED'
  return false
}

const filteredRequests = computed(() => requests.value.filter((r) => matchesTab(r, activeTab.value)))

function countFor(tabKey) {
  const count = requests.value.filter((r) => matchesTab(r, tabKey)).length
  return count > 0 ? count : ''
}

function itemSummary(request) {
  if (request.items.length === 0) return '-'
  const first = request.items[0].flavorName
  return request.items.length > 1 ? `${first} 외 ${request.items.length - 1}종` : first
}

function totalQuantity(request) {
  return request.items.reduce((sum, item) => sum + (item.approvedQuantity ?? item.requestedQuantity), 0)
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

function stepIndex(status) {
  if (status === 'SHIPPING') return 1
  if (status === 'DELIVERED') return 2
  return 0
}

async function onCancel(request) {
  actingId.value = request.stockRequestId
  try {
    await cancelStockRequest(request.stockRequestId)
    await load()
  } finally {
    actingId.value = null
  }
}

async function onConfirmReceipt(request) {
  actingId.value = request.stockRequestId
  try {
    await confirmReceipt(request.stockRequestId)
    await load()
  } finally {
    actingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.page-header h2 {
  margin: 0 0 0.25rem;
}

.page-header p {
  margin: 0 0 1rem;
  color: #6b7280;
  font-size: 0.8125rem;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tabs button {
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 999px;
  padding: 0.375rem 0.875rem;
  font-size: 0.8125rem;
  cursor: pointer;
}

.tabs button.active {
  background: #eef2ff;
  border-color: #4f46e5;
  color: #4f46e5;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.75rem;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.summary {
  display: flex;
  flex-direction: column;
}

.summary span {
  font-size: 0.75rem;
  color: #9ca3af;
}

.timeline {
  display: flex;
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 0.6875rem;
  color: #9ca3af;
}

.timeline li {
  flex: 1;
  text-align: center;
  position: relative;
  padding-top: 1rem;
}

.timeline li::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e5e7eb;
}

.timeline[data-step='0'] li:nth-child(1)::before,
.timeline[data-step='1'] li:nth-child(-n + 2)::before,
.timeline[data-step='2'] li::before {
  background: #4f46e5;
}

.rejection {
  margin: 0;
  font-size: 0.75rem;
  color: #b91c1c;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.actions button {
  border-radius: 8px;
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
  cursor: pointer;
}

.primary {
  border: none;
  background: #059669;
  color: white;
}

.ghost {
  border: 1px solid #e5e7eb;
  background: white;
  color: #374151;
}

.ghost:disabled {
  opacity: 0.5;
  cursor: default;
}

.empty {
  color: #9ca3af;
  grid-column: 1 / -1;
  text-align: center;
}
</style>
