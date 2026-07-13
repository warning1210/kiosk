<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <div class="header-content">
        <h1>지점 주문 대시보드</h1>
        <p class="subtitle">실시간 주문 및 매출 현황</p>
      </div>
      <button class="refresh-btn" @click="fetchData" :class="{ 'spinning': isLoading }">
        새로고침
      </button>
    </header>

    <div class="summary-cards" v-if="summary">
      <div class="card summary-card new-order">
        <div class="card-icon">🆕</div>
        <div class="card-info">
          <h3>신규 주문</h3>
          <p class="value">{{ summary.newOrderCount }}건</p>
        </div>
      </div>
      <div class="card summary-card processing-order">
        <div class="card-icon">👩‍🍳</div>
        <div class="card-info">
          <h3>제조 중</h3>
          <p class="value">{{ summary.processingOrderCount }}건</p>
        </div>
      </div>
      <div class="card summary-card completed-order">
        <div class="card-icon">✅</div>
        <div class="card-info">
          <h3>오늘 완료</h3>
          <p class="value">{{ summary.completedOrderCount }}건</p>
        </div>
      </div>
      <div class="card summary-card total-sales">
        <div class="card-icon">💰</div>
        <div class="card-info">
          <h3>오늘 매출</h3>
          <p class="value">{{ formatCurrency(summary.todaySalesAmount) }}</p>
        </div>
      </div>
    </div>

    <div class="orders-section">
      <h2 class="section-title">실시간 주문 현황</h2>
      
      <div v-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>현재 대기 중인 주문이 없습니다.</p>
      </div>

      <div class="orders-grid">
        <div 
          v-for="order in orders" 
          :key="order.orderId" 
          class="card order-card"
          :class="{ 'urgent': order.elapsedMinutes >= 15 }"
        >
          <div class="order-header">
            <span class="order-number">대기 #{{ order.waitingNumber }}</span>
            <span class="order-time" :class="{ 'warning': order.elapsedMinutes >= 15 }">
              {{ order.elapsedMinutes }}분 전
            </span>
          </div>
          
          <div class="order-body">
            <span class="badge" :class="order.orderType === 'TAKEOUT' ? 'badge-takeout' : 'badge-dinein'">
              {{ order.orderType === 'TAKEOUT' ? '포장' : '매장' }}
            </span>
            <span class="badge status-badge" :class="order.status.toLowerCase()">
              {{ formatStatus(order.status) }}
            </span>
            <h4 class="menu-summary">{{ order.menuSummary }}</h4>
            <p class="order-id">ORD: {{ order.orderNumber }}</p>
          </div>

          <div class="order-actions">
            <button v-if="order.status !== 'COMPLETED'" class="btn btn-complete" @click="changeStatus(order.orderId, 'COMPLETED')">
              제조 완료
            </button>
            <button class="btn btn-cancel" @click="changeStatus(order.orderId, 'CANCELLED')">
              주문 취소
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { fetchDashboardSummary, fetchBranchOrders, updateOrderStatus } from '../../api/branch';

const route = useRoute();
const branchId = route.params.branchId || 1; // 기본값 1

const summary = ref(null);
const orders = ref([]);
const isLoading = ref(false);
let pollingInterval = null;

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(amount || 0);
};

const formatStatus = (status) => {
  if (!status) return '';
  switch(status) {
    case 'PAID': return '신규주문';
    case 'MAKING': return '제조중';
    case 'COMPLETED': return '픽업대기';
    case 'CANCELLED': return '취소됨';
    default: return status;
  }
};

const fetchData = async () => {
  isLoading.value = true;
  try {
    summary.value = await fetchDashboardSummary(branchId);
    orders.value = await fetchBranchOrders(branchId);
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error);
  } finally {
    setTimeout(() => { isLoading.value = false; }, 500);
  }
};

const changeStatus = async (orderId, newStatus) => {
  // BR-021: 주문 취소는 사유를 직접 입력해야 진행된다
  let cancelReason;
  if (newStatus === 'CANCELLED') {
    cancelReason = prompt('취소 사유를 입력해주세요.');
    if (!cancelReason || !cancelReason.trim()) {
      return;
    }
  }

  try {
    await updateOrderStatus(branchId, orderId, newStatus, cancelReason);
    await fetchData(); // 상태 변경 후 데이터 새로고침
  } catch (error) {
    alert('상태 변경에 실패했습니다.');
    console.error(error);
  }
};

onMounted(() => {
  fetchData();
  // 10초마다 자동 갱신
  pollingInterval = setInterval(fetchData, 10000);
});

onUnmounted(() => {
  if (pollingInterval) clearInterval(pollingInterval);
});
</script>

<style scoped>
/* 베스킨라빈스 테마 색상 및 모던 UI CSS */
.dashboard-container {
  padding: 2rem;
  background-color: #f8f9fc;
  min-height: 100vh;
  font-family: 'Pretendard', -apple-system, sans-serif;
  color: #2c3e50;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.dashboard-header h1 {
  font-size: 2rem;
  font-weight: 800;
  margin: 0;
  color: #1a202c;
}

.subtitle {
  color: #718096;
  margin-top: 0.5rem;
}

.refresh-btn {
  background-color: #ffffff;
  color: #FF6B9E;
  border: 2px solid #FF6B9E;
  padding: 0.5rem 1.5rem;
  border-radius: 2rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(255, 107, 158, 0.1);
}

.refresh-btn:hover {
  background-color: #FF6B9E;
  color: white;
}

.refresh-btn.spinning {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 요약 카드 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(0,0,0,0.05);
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.summary-card {
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.card-icon {
  font-size: 2.5rem;
  background: #fdf2f8;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
}

.card-info h3 {
  margin: 0;
  font-size: 0.875rem;
  color: #718096;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.card-info .value {
  margin: 0.5rem 0 0 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #2d3748;
}

.total-sales .card-icon { background: #ebf8ff; }
.processing-order .card-icon { background: #fffaf0; }

/* 주문 리스트 */
.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #edf2f7;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 1rem;
  color: #a0aec0;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.order-card {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.order-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #FF6B9E, #2196F3);
}

.order-card.urgent::before {
  background: #e53e3e;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.order-number {
  font-size: 1.25rem;
  font-weight: 800;
  color: #1a202c;
}

.order-time {
  font-size: 0.875rem;
  color: #718096;
  font-weight: 600;
}

.order-time.warning {
  color: #e53e3e;
  animation: pulse 2s infinite;
}

.order-body {
  margin-bottom: 1.5rem;
  flex-grow: 1;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  margin-right: 0.5rem;
  margin-bottom: 0.5rem;
}

.badge-takeout { background: #ebf8ff; color: #3182ce; }
.badge-dinein { background: #e6fffa; color: #319795; }

.status-badge.paid { background: #fed7d7; color: #c53030; }
.status-badge.making { background: #fefcbf; color: #b7791f; }
.status-badge.completed { background: #c6f6d5; color: #2f855a; }

.menu-summary {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0.5rem 0;
  line-height: 1.4;
}

.order-id {
  font-size: 0.75rem;
  color: #a0aec0;
  margin: 0;
}

.order-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: auto;
}

.btn {
  flex: 1;
  padding: 0.75rem;
  border-radius: 0.5rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-complete {
  background-color: #FF6B9E;
  color: white;
}

.btn-complete:hover { background-color: #FF458A; }

.btn-cancel {
  background-color: #edf2f7;
  color: #4a5568;
}

.btn-cancel:hover { background-color: #e2e8f0; color: #1a202c; }

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

@media (max-width: 768px) {
  .dashboard-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  .refresh-btn { width: 100%; }
}
</style>
