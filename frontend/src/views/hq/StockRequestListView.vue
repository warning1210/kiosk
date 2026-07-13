<template>
  <section>
    <header class="page-header">
      <h2>재고 신청</h2>
      <p>지점에서 본점으로 재고를 신청하고 관리할 수 있습니다.</p>
    </header>

    <div class="kpi-row" v-if="summary">
      <div class="kpi">
        <span>전체 신청</span>
        <strong>{{ summary.totalCount }}</strong>
      </div>
      <div class="kpi">
        <span>신청 대기</span>
        <strong>{{ summary.pendingCount }}</strong>
      </div>
      <div class="kpi">
        <span>승인 완료</span>
        <strong>{{ summary.approvedCount }}</strong>
      </div>
      <div class="kpi">
        <span>반려</span>
        <strong>{{ summary.rejectedCount }}</strong>
      </div>
    </div>

    <div class="toolbar">
      <select v-model="statusFilter" @change="load">
        <option value="">전체 상태</option>
        <option value="PENDING">신청 대기</option>
        <option value="PREPARING">배송 준비</option>
        <option value="SHIPPING">배송중</option>
        <option value="DELIVERED">완료</option>
        <option value="REJECTED">반려</option>
      </select>
      <input v-model="keyword" type="text" placeholder="신청번호, 지점명, 상품명 검색" @keyup.enter="load" />
      <button type="button" class="ghost" @click="load">검색</button>
    </div>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="banner danger">{{ error }}</p>

    <table v-else class="request-table">
      <thead>
        <tr>
          <th>신청 번호</th>
          <th>신청 지점</th>
          <th>신청 상품</th>
          <th>수량</th>
          <th>신청일</th>
          <th>상태</th>
          <th>처리</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="request in requests" :key="request.stockRequestId">
          <td>{{ request.requestNumber }}</td>
          <td>{{ request.branchName }}</td>
          <td>{{ itemSummary(request) }}</td>
          <td>{{ totalQuantity(request) }}개</td>
          <td>{{ formatDateTime(request.requestedAt) }}</td>
          <td><span class="badge" :class="statusClass(request.requestStatus)">{{ statusLabel(request.requestStatus) }}</span></td>
          <td class="actions">
            <template v-if="request.requestStatus === 'PENDING'">
              <button type="button" class="approve" :disabled="actingId === request.stockRequestId" @click="onApprove(request)">승인</button>
              <button type="button" class="reject" :disabled="actingId === request.stockRequestId" @click="openReject(request)">반려</button>
            </template>
            <button v-else-if="request.requestStatus === 'PREPARING'" type="button" class="ghost" @click="openShip(request)">
              배송 등록
            </button>
          </td>
        </tr>
        <tr v-if="requests.length === 0">
          <td colspan="7" class="empty">조건에 맞는 신청 건이 없습니다</td>
        </tr>
      </tbody>
    </table>

    <RejectModal v-if="rejectTarget" @close="rejectTarget = null" @confirm="onReject" />
    <ShipModal v-if="shipTarget" @close="shipTarget = null" @confirm="onShip" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  approveStockRequest,
  fetchHqStockRequestSummary,
  fetchHqStockRequests,
  rejectStockRequest,
  shipStockRequest
} from '../../api/hqStockRequest'
import RejectModal from './RejectModal.vue'
import ShipModal from './ShipModal.vue'

const requests = ref([])
const summary = ref(null)
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')
const keyword = ref('')
const actingId = ref(null)
const rejectTarget = ref(null)
const shipTarget = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [page, summaryData] = await Promise.all([
      fetchHqStockRequests({ status: statusFilter.value || undefined, keyword: keyword.value || undefined, size: 50 }),
      fetchHqStockRequestSummary()
    ])
    requests.value = page.content
    summary.value = summaryData
  } catch (e) {
    error.value = e.response?.data?.error?.message ?? '신청 목록을 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

function itemSummary(request) {
  if (request.items.length === 0) return '-'
  const first = request.items[0].flavorName
  return request.items.length > 1 ? `${first} 외 ${request.items.length - 1}종` : first
}

function totalQuantity(request) {
  return request.items.reduce((sum, item) => sum + (item.approvedQuantity ?? item.requestedQuantity), 0)
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function statusLabel(status) {
  return { PENDING: '신청 대기', PREPARING: '배송 준비', SHIPPING: '배송중', DELIVERED: '완료', REJECTED: '반려', CLOSED: '취소' }[status] ?? status
}

function statusClass(status) {
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  if (status === 'DELIVERED') return 'success'
  return 'info'
}

async function onApprove(request) {
  actingId.value = request.stockRequestId
  try {
    await approveStockRequest(request.stockRequestId)
    await load()
  } finally {
    actingId.value = null
  }
}

function openReject(request) {
  rejectTarget.value = request
}

async function onReject(reason) {
  const request = rejectTarget.value
  actingId.value = request.stockRequestId
  try {
    await rejectStockRequest(request.stockRequestId, reason)
    rejectTarget.value = null
    await load()
  } finally {
    actingId.value = null
  }
}

function openShip(request) {
  shipTarget.value = request
}

async function onShip(payload) {
  const request = shipTarget.value
  actingId.value = request.stockRequestId
  try {
    await shipStockRequest(request.stockRequestId, payload)
    shipTarget.value = null
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

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.kpi {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.kpi span {
  font-size: 0.8125rem;
  color: #6b7280;
}

.kpi strong {
  font-size: 1.5rem;
}

.toolbar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.toolbar select,
.toolbar input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
}

.toolbar input {
  flex: 1;
}

.banner.danger {
  background: #fef2f2;
  color: #b91c1c;
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
}

.request-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.request-table th,
.request-table td {
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

.badge.warning {
  background: #fffbeb;
  color: #b45309;
}

.badge.danger {
  background: #fef2f2;
  color: #b91c1c;
}

.badge.success {
  background: #ecfdf5;
  color: #059669;
}

.badge.info {
  background: #eef2ff;
  color: #4f46e5;
}

.actions {
  display: flex;
  gap: 0.375rem;
}

.actions button {
  border-radius: 6px;
  padding: 0.3125rem 0.625rem;
  font-size: 0.8125rem;
  cursor: pointer;
}

.approve {
  border: none;
  background: #059669;
  color: white;
}

.reject {
  border: none;
  background: #dc2626;
  color: white;
}

.ghost {
  border: 1px solid #e5e7eb;
  background: white;
  color: #374151;
}

.empty {
  text-align: center;
  color: #9ca3af;
}
</style>
