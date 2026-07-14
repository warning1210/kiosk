<template>
  <section>
    <!-- 이 화면이 무엇을 보여 주는지 알려 주는 제목 영역 -->
    <header class="page-header">
      <h2>입고·신청 현황</h2>
      <p>신청한 재고의 배송 상태 · 배송 완료 시 재고에 자동 반영</p>
    </header>

    <!-- 탭을 누르면 activeTab만 바뀌고, computed 목록이 즉시 다시 계산된다. -->
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

    <!-- 요청 중, 오류, 정상 목록은 동시에 보이지 않도록 v-if 계열로 분기한다. -->
    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="banner danger">{{ error }}</p>

    <div v-else class="cards">
      <!--
        stockRequestId는 DB에서 정해진 신청의 고유 ID다.
        목록 순서가 달라져도 같은 신청을 같은 DOM으로 추적할 수 있어 안정적인 key가 된다.
      -->
      <article v-for="request in filteredRequests" :key="request.stockRequestId" class="card">
        <!-- 여러 상품을 짧은 이름과 총수량으로 요약해서 카드의 핵심 정보를 보여 준다. -->
        <div class="summary">
          <strong>{{ summarizeStockRequestItems(request) }}</strong>
          <span>{{ formatDate(request.requestedAt) }} 신청 · {{ calculateStockRequestQuantity(request) }}개</span>
        </div>

        <!-- data-step 값을 CSS가 읽어서 현재 단계까지의 점 색상을 채운다. -->
        <ol class="timeline" :data-step="stepIndex(request.requestStatus)">
          <li>신청</li>
          <li>배송중</li>
          <li>수령확인</li>
        </ol>

        <p v-if="request.requestStatus === 'REJECTED'" class="rejection">반려: {{ request.rejectionReason }}</p>

        <!-- 현재 업무 상태에서 허용되는 버튼만 보여 주어 잘못된 상태 변경을 예방한다. -->
        <div class="actions">
          <button
            v-if="request.requestStatus === 'PENDING'"
            type="button"
            class="ghost"
            :disabled="actingId === request.stockRequestId"
            @click="cancelRequest(request)"
          >
            신청 취소
          </button>
          <button
            v-else-if="request.requestStatus === 'SHIPPING'"
            type="button"
            class="primary"
            :disabled="actingId === request.stockRequestId"
            @click="confirmRequestReceipt(request)"
          >
            배송완료 확인
          </button>
          <button v-else-if="request.requestStatus === 'DELIVERED'" type="button" class="ghost" disabled>
            재고 반영됨
          </button>
        </div>
      </article>

      <!-- 필터 결과가 비어 있을 때 빈 화면 대신 이유를 알려 준다. -->
      <p v-if="filteredRequests.length === 0" class="empty">해당 상태의 신청 건이 없습니다</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { cancelStockRequest, confirmReceipt, fetchBranchStockRequests } from '../../api/branchStockRequest'
import { calculateStockRequestQuantity, summarizeStockRequestItems } from '../../utils/stockRequestDisplay'

// 화면 탭의 값과 서버 상태를 분리해 둔다.
// REQUESTED 탭은 아직 지점에 도착하지 않은 PENDING과 PREPARING을 함께 보여 준다.
const tabs = [
  { key: 'ALL', label: '전체' },
  { key: 'REQUESTED', label: '신청됨' },
  { key: 'SHIPPING', label: '배송중' },
  { key: 'DELIVERED', label: '완료' }
]

// 서버 원본 목록과 화면 제어 상태를 각각 ref로 관리한다.
const requests = ref([])
const loading = ref(true)
const error = ref('')
const activeTab = ref('ALL')

// actingId에는 현재 상태 변경 요청 중인 신청 ID만 저장한다.
// 목록 전체가 아니라 해당 카드의 버튼만 비활성화할 때 사용한다.
const actingId = ref(null)

// 백엔드의 페이지 응답 중 실제 배열인 content를 화면 상태에 저장한다.
async function loadStockRequests() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchBranchStockRequests({ size: 100 })
    requests.value = page.content
  } catch (e) {
    error.value = e.response?.data?.message ?? '신청 현황을 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

// 하나의 판별 함수를 필터링과 탭별 개수 계산에서 함께 사용해 기준 불일치를 막는다.
function matchesTab(request, tabKey) {
  if (tabKey === 'ALL') return true
  if (tabKey === 'REQUESTED') return request.requestStatus === 'PENDING' || request.requestStatus === 'PREPARING'
  if (tabKey === 'SHIPPING') return request.requestStatus === 'SHIPPING'
  if (tabKey === 'DELIVERED') return request.requestStatus === 'DELIVERED'
  return false
}

// computed는 requests 또는 activeTab이 바뀔 때만 필터 결과를 다시 계산한다.
// 서버를 다시 호출하지 않고 이미 받은 목록에서 탭 화면을 빠르게 전환한다.
const filteredRequests = computed(() => requests.value.filter((r) => matchesTab(r, activeTab.value)))

// 개수가 0이면 숫자를 숨겨 탭 이름만 표시한다.
function countFor(tabKey) {
  const count = requests.value.filter((r) => matchesTab(r, tabKey)).length
  return count > 0 ? count : ''
}

// 서버의 ISO 날짜 문자열을 카드에 필요한 월.일 형식으로 바꾼다.
function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

// 업무 상태를 화면 타임라인의 0부터 시작하는 단계 번호로 변환한다.
function stepIndex(status) {
  if (status === 'SHIPPING') return 1
  if (status === 'DELIVERED') return 2
  return 0
}

// 취소가 성공하면 서버 목록을 다시 받아 화면을 실제 서버 상태와 맞춘다.
async function cancelRequest(request) {
  actingId.value = request.stockRequestId
  try {
    await cancelStockRequest(request.stockRequestId)
    await loadStockRequests()
  } catch (e) {
    error.value = e.response?.data?.message ?? '신청을 취소하지 못했습니다'
  } finally {
    actingId.value = null
  }
}

// 수령 확인은 백엔드에서 신청 완료와 지점 재고 반영을 함께 처리한다.
// 성공 후 새 목록을 조회하므로 카드도 DELIVERED 상태로 갱신된다.
async function confirmRequestReceipt(request) {
  actingId.value = request.stockRequestId
  try {
    await confirmReceipt(request.stockRequestId)
    await loadStockRequests()
  } catch (e) {
    error.value = e.response?.data?.message ?? '수령 확인에 실패했습니다'
  } finally {
    actingId.value = null
  }
}

// 컴포넌트가 화면에 처음 붙은 직후 최초 목록을 한 번 조회한다.
onMounted(loadStockRequests)
</script>

<style scoped>
/* 페이지 제목과 설명 */
.page-header h2 {
  margin: 0 0 0.25rem;
}

.page-header p {
  margin: 0 0 1rem;
  color: #6b7280;
  font-size: 0.8125rem;
}

/* 상태별 필터 탭 */
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

/* 신청 카드는 화면 폭에 맞춰 열 수가 자동으로 달라지는 반응형 그리드다. */
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

/* 카드의 상품 요약 정보 */
.summary {
  display: flex;
  flex-direction: column;
}

.summary span {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* 타임라인을 세 단계로 나누고 data-step에 따라 현재 단계까지의 점을 강조한다. */
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

/* 반려 사유와 상태별 실행 버튼 */
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

/* 필터 결과가 없을 때 그리드 전체 너비를 사용하는 안내 문구 */
.empty {
  color: #9ca3af;
  grid-column: 1 / -1;
  text-align: center;
}
</style>
