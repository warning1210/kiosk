<template>
  <section>
    <!-- 본점 담당자가 이 화면에서 처리할 업무를 알려 주는 제목 영역 -->
    <header class="page-header">
      <h2>재고 신청</h2>
      <p>지점에서 본점으로 재고를 신청하고 관리할 수 있습니다.</p>
    </header>

    <!-- 요약 API 응답이 도착한 뒤에만 전체/대기/승인 후 처리/반려 건수를 표시한다. -->
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

    <!--
      상태 선택은 change 즉시 조회하고, 검색어는 Enter 또는 검색 버튼으로 조회한다.
      v-model이 사용자의 입력값을 statusFilter와 keyword에 양방향으로 연결한다.
    -->
    <div class="toolbar">
      <select v-model="statusFilter" @change="loadStockRequests">
        <option value="">전체 상태</option>
        <option value="PENDING">신청 대기</option>
        <option value="PREPARING">배송 준비</option>
        <option value="SHIPPING">배송중</option>
        <option value="DELIVERED">완료</option>
        <option value="REJECTED">반려</option>
      </select>
      <input v-model="keyword" type="text" placeholder="신청번호, 지점명, 상품명 검색" @keyup.enter="loadStockRequests" />
      <button type="button" class="ghost" @click="loadStockRequests">검색</button>
    </div>

    <!-- 로딩과 오류가 끝난 정상 상태에서만 신청 테이블을 렌더링한다. -->
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
        <!-- DB의 PK인 stockRequestId를 key로 써서 정렬/검색 후에도 행의 정체성을 유지한다. -->
        <tr v-for="request in requests" :key="request.stockRequestId">
          <td>{{ request.requestNumber }}</td>
          <td>{{ request.branchName }}</td>
          <td>{{ summarizeStockRequestItems(request) }}</td>
          <td>{{ calculateStockRequestQuantity(request) }}개</td>
          <td>{{ formatDateTime(request.requestedAt) }}</td>
          <td><span class="badge" :class="statusClass(request.requestStatus)">{{ statusLabel(request.requestStatus) }}</span></td>
          <td class="actions">
            <!-- 백엔드 상태 전이 규칙에 맞춰 현재 가능한 처리 버튼만 노출한다. -->
            <template v-if="request.requestStatus === 'PENDING'">
              <button type="button" class="approve" :disabled="actingId === request.stockRequestId" @click="approveRequest(request)">승인</button>
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

    <!--
      선택한 신청 객체가 있을 때만 해당 모달을 만든다.
      자식 모달의 close/confirm 이벤트를 받아 선택을 해제하거나 실제 API 요청을 시작한다.
    -->
    <RejectModal
      v-if="rejectTarget"
      :submitting="actingId === rejectTarget.stockRequestId"
      @close="rejectTarget = null"
      @confirm="onReject"
    />
    <ShipModal
      v-if="shipTarget"
      :submitting="actingId === shipTarget.stockRequestId"
      @close="shipTarget = null"
      @confirm="onShip"
    />
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
import { calculateStockRequestQuantity, summarizeStockRequestItems } from '../../utils/stockRequestDisplay'
import RejectModal from './RejectModal.vue'
import ShipModal from './ShipModal.vue'

// 조회 결과와 상단 KPI 요약 데이터다. ref가 변경되면 이를 사용하는 template도 갱신된다.
const requests = ref([])
const summary = ref(null)

// 목록 화면의 로딩/오류/검색 조건을 나타내는 상태다.
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')
const keyword = ref('')

// actingId는 처리 중인 행의 버튼을 잠그고, Target은 어느 신청의 모달을 열지 결정한다.
const actingId = ref(null)
const rejectTarget = ref(null)
const shipTarget = ref(null)

// 목록과 요약은 서로 의존하지 않으므로 Promise.all로 동시에 요청한다.
// 두 요청이 모두 성공하면 두 상태를 갱신하고, 하나라도 실패하면 이번 조회 결과는 반영하지 않는다.
async function loadStockRequests() {
  loading.value = true
  error.value = ''
  try {
    const [page, summaryData] = await Promise.all([
      // 빈 검색 조건은 undefined로 보내 Axios가 불필요한 쿼리 파라미터를 생략하게 한다.
      fetchHqStockRequests({ status: statusFilter.value || undefined, keyword: keyword.value || undefined, size: 50 }),
      fetchHqStockRequestSummary()
    ])
    requests.value = page.content
    summary.value = summaryData
  } catch (e) {
    error.value = e.response?.data?.message ?? '신청 목록을 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

// 서버 날짜를 본점 목록에서 읽기 쉬운 연.월.일 시:분 형식으로 변환한다.
function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// 서버 enum 값은 API 계약에 그대로 두고, 사용자에게 보이는 한국어만 화면에서 대응시킨다.
function statusLabel(status) {
  return { PENDING: '신청 대기', PREPARING: '배송 준비', SHIPPING: '배송중', DELIVERED: '완료', REJECTED: '반려', CLOSED: '취소' }[status] ?? status
}

// 상태에 알맞은 CSS 클래스명을 돌려 배지 색상만 담당하게 한다.
function statusClass(status) {
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  if (status === 'DELIVERED') return 'success'
  return 'info'
}

// 승인 중에는 같은 행의 승인/반려 버튼을 잠그고, 승인이 성공하면 서버 데이터를 다시 조회한다.
async function approveRequest(request) {
  actingId.value = request.stockRequestId
  try {
    await approveStockRequest(request.stockRequestId)
    await loadStockRequests()
  } catch (e) {
    error.value = e.response?.data?.message ?? '승인 처리에 실패했습니다'
  } finally {
    actingId.value = null
  }
}

// 모달을 여는 함수는 선택 대상만 저장하고, 실제 반려 API 요청은 onReject가 담당한다.
function openReject(request) {
  rejectTarget.value = request
}

// RejectModal이 전달한 사유를 백엔드 요청 본문으로 보내고 성공한 경우에만 모달을 닫는다.
async function onReject(reason) {
  const request = rejectTarget.value
  actingId.value = request.stockRequestId
  try {
    await rejectStockRequest(request.stockRequestId, reason)
    rejectTarget.value = null
    await loadStockRequests()
  } catch (e) {
    error.value = e.response?.data?.message ?? '반려 처리에 실패했습니다'
  } finally {
    actingId.value = null
  }
}

// 배송 등록도 반려와 같은 방식으로 대상 선택과 실제 요청을 분리한다.
function openShip(request) {
  shipTarget.value = request
}

// ShipModal의 송장/배송 정보를 그대로 API 계층에 전달한 뒤 목록과 KPI를 함께 갱신한다.
async function onShip(payload) {
  const request = shipTarget.value
  actingId.value = request.stockRequestId
  try {
    await shipStockRequest(request.stockRequestId, payload)
    shipTarget.value = null
    await loadStockRequests()
  } catch (e) {
    error.value = e.response?.data?.message ?? '배송 등록에 실패했습니다'
  } finally {
    actingId.value = null
  }
}

// 화면에 처음 들어왔을 때 필터가 없는 기본 목록과 KPI를 조회한다.
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

/* 네 가지 요약 수치를 같은 너비로 배치한다. */
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

/* 상태 필터와 검색 입력을 한 줄 도구 모음으로 배치한다. */
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

/* 서버 요청 실패 안내 배너 */
.banner.danger {
  background: #fef2f2;
  color: #b91c1c;
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
}

/* 신청 목록 표와 셀의 기본 모양 */
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

/* 업무 상태는 글자만 읽지 않아도 구분할 수 있도록 배지 색상을 함께 사용한다. */
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

/* 승인, 반려, 배송 등록 버튼 영역 */
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

/* 검색 결과가 없을 때 표시하는 표 내부 안내 */
.empty {
  text-align: center;
  color: #9ca3af;
}
</style>
