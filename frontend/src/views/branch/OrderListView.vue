<template>
  <div class="shell">
    <BranchSidebar active="orderlist" />
    <main class="split-layout">
      <!-- Left side: List -->
      <section class="list-section">
        <header>
          <div>
            <h1>주문 내역</h1>
            <p>완료/취소된 주문 내역을 확인합니다</p>
          </div>
          <div class="date-picker-wrap">
            <VueDatePicker v-model="filterDate" :enable-time-picker="false" :allowed-dates="availableDates" format="yyyy-MM-dd" model-type="yyyy-MM-dd" auto-apply class="date-input" @update:model-value="load" />
          </div>
        </header>
        <div class="list-container">
          <table>
            <thead>
              <tr>
                <th>주문번호</th>
                <th>유형</th>
                <th>메뉴</th>
                <th>상태</th>
                <th>금액</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in sorted" :key="o.orderId" 
                  @click="selectOrder(o.orderId)"
                  :class="{ active: selectedOrderId === o.orderId }">
                <td><b>#{{ String(o.waitingNumber).padStart(3, '0') }}</b></td>
                <td><span class="type" :class="o.orderType.toLowerCase()">{{ o.orderType === 'TAKEOUT' ? '포장' : '매장' }}</span></td>
                <td class="menu-summary">{{ o.menuSummary }}</td>
                <td><strong :class="`s-${o.status}`">{{ label(o.status) }}</strong></td>
                <td>{{ o.finalAmount ? o.finalAmount.toLocaleString() + '원' : '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!sorted.length" class="empty">주문 내역이 없습니다.</div>
        </div>
      </section>

      <!-- Right side: Detail -->
      <section class="detail-section" :class="{ 'is-open': selectedOrder }">
        <div class="detail-wrapper">
          <div v-if="selectedOrder" class="detail-card">
            <div class="detail-header">
              <div class="amount-title">총 결제금액</div>
              <div class="total-amount">{{ selectedOrder.finalAmount ? selectedOrder.finalAmount.toLocaleString() : '0' }}원</div>
            </div>
            
            <div class="actions">
              <button class="btn-change">결제수단 변경</button>
              <button class="btn-receipt">영수증 출력</button>
              <button class="btn-cancel" v-if="selectedOrder.status !== 'CANCELLED'">결제 취소</button>
            </div>

            <div class="info-table" v-if="selectedOrder.payment">
              <div class="info-row"><span class="info-label">금액</span><span class="info-value">{{ selectedOrder.payment.paidAmount?.toLocaleString() }}</span></div>
              <div class="info-row"><span class="info-label">결제시간</span><span class="info-value">{{ formatTime(selectedOrder.payment.paidAt) }}</span></div>
              <div class="info-row"><span class="info-label">결제수단</span><span class="info-value">{{ selectedOrder.payment.paymentMethod }}</span></div>
              <div class="info-row"><span class="info-label">결제구분</span><span class="info-value">{{ selectedOrder.payment.installment }}</span></div>
              <div class="info-row"><span class="info-label">승인번호</span><span class="info-value">{{ selectedOrder.payment.approvalNumber }}</span></div>
              <div class="info-row"><span class="info-label">승인상태</span><span class="info-value">{{ selectedOrder.payment.paymentStatus }}</span></div>
            </div>
            <div class="info-table" v-else>
              <div class="empty-info">결제 정보가 없습니다. (전액 포인트 결제 등)</div>
            </div>

            <div class="items-list">
              <h3 class="items-title">주문내역</h3>
              <div class="item" v-for="(item, i) in selectedOrder.items" :key="i">
                <div class="item-header">
                  <span class="item-name">{{ item.productName }} × {{ item.quantity }}</span>
                  <span class="item-price">{{ item.itemTotal?.toLocaleString() }}</span>
                </div>
                <div class="item-options" v-if="item.options && item.options.length">
                  {{ item.options.join(' / ') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import http from '../../api/branch';
import BranchSidebar from '../../components/branch/BranchSidebar.vue';
import { VueDatePicker } from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';

const orders = ref([]);
const filteredOrders = computed(() => orders.value.filter(o => o.status === 'COMPLETED' || o.status === 'CANCELLED'));
const sorted = computed(() => [...filteredOrders.value].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)));

const selectedOrderId = ref(null);
const selectedOrder = ref(null);

// 오늘 날짜를 기본값으로 설정
const today = new Date();
const filterDate = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`);
const availableDates = ref([]);

async function loadDates() {
  try {
    const res = await http.get('/orders/dates');
    availableDates.value = res.data.map(d => new Date(d));
  } catch(e) {
    console.error(e);
  }
}

function label(s) {
  return ({ PAID: '신규', MAKING: '처리중', READY: '준비완료', COMPLETED: '완료', CANCELLED: '취소' })[s] || s;
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

async function load() {
  try {
    const url = filterDate.value ? `/orders?date=${filterDate.value}` : '/orders';
    orders.value = (await http.get(url)).data;
    
    // 리스트 새로고침 시 선택된 내역 초기화
    selectedOrderId.value = null;
    selectedOrder.value = null;
  } catch (e) {
    console.error(e);
  }
}

async function selectOrder(orderId) {
  selectedOrderId.value = orderId;
  try {
    const res = await http.get(`/orders/${orderId}`);
    selectedOrder.value = res.data;
  } catch(e) {
    console.error('상세 내역을 불러오는데 실패했습니다.', e);
  }
}

onMounted(() => {
  load();
  loadDates();
});
</script>

<style>
/* VueDatePicker 스타일 커스텀 */
.dp__theme_light {
  --dp-border-radius: 10px;
  --dp-input-padding: 10px 16px;
  --dp-font-size: 14px;
}
.date-picker-wrap .dp__input {
  border: 1px solid #e0e5ec;
  font-family: inherit;
  font-weight: 600;
  color: #1f2938;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  transition: all 0.2s ease;
}
.date-picker-wrap .dp__input:hover {
  border-color: #6165ee;
  box-shadow: 0 4px 12px rgba(97, 101, 238, 0.15);
}
.date-picker-wrap .dp__input:focus {
  border-color: #6165ee;
  box-shadow: 0 0 0 3px rgba(97, 101, 238, 0.2);
}
</style>

<style scoped>
.shell { min-height: 100vh; color: #202a39; background: #f3f6fa; display: flex; }
main.split-layout { 
  margin-left: 238px; 
  padding: 30px; 
  display: flex; 
  width: calc(100% - 238px);
  box-sizing: border-box;
  height: 100vh;
  overflow: hidden;
}
.list-section { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  min-width: 450px;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}
.detail-section { 
  width: 0;
  opacity: 0;
  transform: translateX(40px);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  overflow: hidden;
}
.detail-section.is-open {
  width: 420px;
  opacity: 1;
  transform: translateX(0);
  margin-left: 20px;
}
.detail-wrapper {
  width: 420px; /* Keeps card size fixed during transition */
  height: 100%;
}

header { margin-bottom: 20px; display: flex; justify-content: space-between; align-items: flex-start; }
h1 { margin: 0; font-size: 26px; }
header p { margin: 7px 0 0; color: #7c8796; font-size: 11px; }

.date-input {
  padding: 10px 16px;
  border: 1px solid #e0e5ec;
  border-radius: 10px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #1f2938;
  background-color: #ffffff;
  outline: none;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  transition: all 0.2s ease;
}
.date-input:hover {
  border-color: #6165ee;
  box-shadow: 0 4px 12px rgba(97, 101, 238, 0.15);
}
.date-input:focus { 
  border-color: #6165ee; 
  box-shadow: 0 0 0 3px rgba(97, 101, 238, 0.2);
}
.date-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}
.date-input::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}

.list-container {
  flex: 1;
  background: #fff; 
  border: 1px solid #e0e5ec; 
  border-radius: 15px;
  overflow: auto;
}
table { width: 100%; border-collapse: collapse; }
th { padding: 14px 18px; color: #747f8f; background: #f8fafc; font-size: 11px; text-align: left; position: sticky; top: 0; z-index: 1; }
td { padding: 15px 18px; border-top: 1px solid #edf0f4; font-size: 11px; }
td > b { font-size: 13px; }
tr { cursor: pointer; transition: background 0.2s; }
tr:hover { background: #f9fafb; }
tr.active { background: #f0f4ff; }
.menu-summary { max-width: 150px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type { padding: 5px 8px; border-radius: 6px; font-weight: 800; }
.type.dine_in { color: #3671e9; background: #eaf1ff; }
.type.takeout { color: #7253e7; background: #f0ebff; }
.s-PAID { color: #6165ee; }
.s-MAKING { color: #dc7200; }
.s-COMPLETED { color: #0b9c58; }
.s-CANCELLED { color: #a3a9b2; }

.empty { padding: 60px; text-align: center; color: #9ba3af; }

/* Detail Card Styles */
.detail-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  border: 1px solid #e0e5ec;
  padding: 30px;
  overflow-y: auto;
  height: 100%;
  box-sizing: border-box;
}
.detail-card::-webkit-scrollbar { width: 6px; }
.detail-card::-webkit-scrollbar-thumb { background: #dce1e8; border-radius: 3px; }
.detail-header { margin-bottom: 20px; }
.amount-title { font-size: 12px; color: #7c8796; margin-bottom: 4px; }
.total-amount { font-size: 28px; font-weight: 800; color: #1f2938; }

.actions { display: flex; gap: 8px; margin-bottom: 30px; justify-content: flex-end; }
.actions button { 
  padding: 8px 12px; 
  border-radius: 6px; 
  font-size: 11px; 
  font-weight: bold; 
  cursor: pointer;
  border: none;
}
.btn-change { background: #eef2f6; color: #566173; }
.btn-receipt { background: #eaf1ff; color: #3671e9; }
.btn-cancel { background: #ffe8eb; color: #ef3948; }

.info-table { border-top: 1px solid #edf0f4; border-bottom: 1px solid #edf0f4; padding: 20px 0; margin-bottom: 30px; }
.info-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 12px; }
.info-row:last-child { margin-bottom: 0; }
.info-label { color: #7c8796; }
.info-value { color: #1f2938; font-weight: 500; text-align: right; }
.empty-info { text-align: center; color: #9ba3af; font-size: 12px; }

.items-title { font-size: 14px; font-weight: 700; color: #1f2938; margin: 0 0 16px; }
.item { margin-bottom: 16px; }
.item-header { display: flex; justify-content: space-between; font-size: 13px; font-weight: bold; color: #1f2938; margin-bottom: 4px; }
.item-options { font-size: 11px; color: #9ba3af; line-height: 1.4; }



@media(max-width: 1024px) {
  main.split-layout { margin-left: 0; width: 100%; flex-direction: column; height: auto; padding: 20px; }
  .list-section { min-width: 100%; height: 400px; }
  .detail-section { width: 100%; height: auto; min-height: 400px; }
}
</style>
