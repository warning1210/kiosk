<template>
  <div class="shell">
    <BranchSidebar active="orders" />
    <main>
      <header>
        <div>
          <h1>주문 관리</h1>
          <p>접수 시간이 오래된 주문부터 처리하세요</p>
        </div>
        <div class="header-actions">
          <button class="sort-btn" @click="oldest = !oldest">☷　{{ oldest ? '오래된 순' : '최신 순' }}</button>
          
          <div class="busy-wrapper">
            <button class="busy" :class="{ active: waitTime > 0 }" @click="openPopup">
              <svg v-if="waitTime > 0" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
              </svg>
              {{ waitTime > 0 ? `${waitTime}분` : '바빠요' }}
            </button>
            
            <div v-if="showPopup" class="busy-popup">
              <h3>키오스크 대기시간 표시</h3>
              <p>주문 지연 시 키오스크 메인 화면에 예상 소요시간을 안내합니다.</p>
              
              <div class="time-options">
                <button :class="{ active: tempWaitTime === 10 }" @click="tempWaitTime = 10">10분</button>
                <button :class="{ active: tempWaitTime === 20 }" @click="tempWaitTime = 20">20분</button>
                <button :class="{ active: tempWaitTime === 30 }" @click="tempWaitTime = 30">30분</button>
              </div>
              
              <button class="apply-btn" @click="applyWaitTime">키오스크에 표시</button>
              <button v-if="waitTime > 0" class="clear-btn" @click="clearWaitTime">표시 해제</button>
            </div>
          </div>
        </div>
      </header>
      <section>
        <table>
          <thead>
            <tr>
              <th>주문번호</th>
              <th>경과 시간</th>
              <th>유형</th>
              <th>메뉴</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="o in sorted" :key="o.orderId">
              <tr @click="toggleDetail(o.orderId)" class="clickable-row" :class="{ expanded: expandedOrderId === o.orderId }">
                <td>
                  <!-- 과거 주문 데이터에 waitingNumber가 없어도 화면에 null을 노출하지 않는다. -->
                  <b>#{{ formatWaitingNumber(o) }}</b>
                  <span v-if="o.paymentMethod === 'CASH'" class="cash-badge">현금</span>
                </td>
                <td>
                  <span class="elapsed" :class="o.elapsedMinutes > 15 ? 'danger' : o.elapsedMinutes > 8 ? 'warn' : 'safe'">
                    {{ o.elapsedMinutes }}분
                  </span>
                </td>
                <td>
                  <span class="type" :class="o.orderType.toLowerCase()">{{ o.orderType === 'TAKEOUT' ? '포장' : '매장' }}</span>
                </td>
                <td>{{ o.menuSummary }}</td>
                <td><strong :class="`s-${o.status}`">{{ label(o.status) }}</strong></td>
                <td>
                  <div class="actions">
                    <!-- 현금 주문은 직원이 실제 돈을 받은 뒤에만 결제 완료로 전환한다. -->
                    <button v-if="o.status === 'PENDING_PAYMENT' && o.paymentMethod === 'CASH'" class="cash-confirm" @click.stop="confirmCash(o)">현금 결제 확인</button>
                    <button v-if="o.status === 'PAID'" class="start" @click.stop="changeStatus(o, 'MAKING')">제조 시작</button>
                    <button v-if="o.status === 'MAKING'" class="complete" @click.stop="changeStatus(o, 'COMPLETED')">완료</button>
                    <button v-if="o.status !== 'CANCELLED'" class="cancel" @click.stop="openCancelModal(o)">취소</button>
                  </div>
                </td>
              </tr>
              <tr v-if="expandedOrderId === o.orderId" class="detail-row">
                <td colspan="6" class="detail-td">
                  <div class="detail-container" v-if="detailOrder">
                    <ul class="order-items">
                      <li v-for="(item, idx) in detailOrder.items" :key="idx">
                        <div class="item-main">
                          <span class="item-name">{{ item.productName }} x {{ item.quantity }}</span>
                          <span class="item-price">{{ item.itemTotal?.toLocaleString() }}원</span>
                        </div>
                        <div class="item-details" v-if="item.options && item.options.length">
                          <div class="flavors-wrapper" v-if="getFlavors(item.options).length">
                            <span class="detail-label">선택한 맛</span>
                            <div class="flavor-tags">
                              <span class="flavor-tag" v-for="f in getFlavors(item.options)" :key="f">{{ f }}</span>
                            </div>
                          </div>
                          <div class="extras-wrapper" v-if="getExtras(item.options).length">
                            <span class="detail-label">포장 및 추가 옵션</span>
                            <div class="extra-tags">
                              <span class="extra-tag" v-for="e in getExtras(item.options)" :key="e">{{ e }}</span>
                            </div>
                          </div>
                        </div>
                      </li>
                    </ul>
                    <div class="order-summary">
                      <div class="summary-row">
                        <span>주문 금액</span>
                        <span>{{ detailOrder.amountBeforeDiscount?.toLocaleString() }}원</span>
                      </div>
                      <div class="summary-row" v-if="detailOrder.discountAmount">
                        <span>할인 금액</span>
                        <span class="discount">-{{ detailOrder.discountAmount?.toLocaleString() }}원</span>
                      </div>
                      <div class="summary-row total">
                        <span>최종 결제 금액</span>
                        <span>{{ detailOrder.finalAmount?.toLocaleString() }}원</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="loading-state">
                    상세 정보를 불러오는 중입니다...
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <div v-if="!sorted.length" class="empty">접수된 주문이 없습니다.</div>
      </section>
    </main>
    <!-- 주문 취소 모달 -->
    <div v-if="showCancelModal" class="modal-overlay" @click.self="closeCancelModal">
      <div class="modal-content">
        <div class="modal-header">
          <div class="icon-circle">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
              <path d="M18 6L6 18M6 6l12 12"></path>
            </svg>
          </div>
          <div class="modal-title-group">
            <h2>주문 취소</h2>
            <p>#{{ cancelTarget?.waitingNumber }} · {{ cancelTarget?.orderType === 'TAKEOUT' ? '포장' : '매장' }} · {{ cancelTarget?.menuSummary }}</p>
          </div>
        </div>
        <div class="modal-body">
          <label>취소 사유 <span class="required">*</span></label>
          <textarea v-model="cancelReasonText" placeholder="재료 소진으로 조리 불가 — 고객 안내 후 환불 처리했습니다."></textarea>
        </div>
        <div class="modal-footer">
          <button class="btn-close" @click="closeCancelModal">닫기</button>
          <button class="btn-cancel-submit" @click="submitCancel">주문 취소</button>
        </div>
      </div>
    </div>

    </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import http from '../../api/branch';
import BranchSidebar from '../../components/branch/BranchSidebar.vue';
import { useBranchEventStream } from '../../composables/useBranchEventStream';
const { onOrder } = useBranchEventStream();
let unsubscribeOrder = null;
const orders = ref([]);
const oldest = ref(true);
const waitTime = ref(0);
const showPopup = ref(false);
const tempWaitTime = ref(20);
const showCancelModal = ref(false);
const cancelTarget = ref(null);
const cancelReasonText = ref('');
const expandedOrderId = ref(null);
const detailOrder = ref(null);
const newCount = computed(() => orders.value.filter(o => o.status === 'PAID').length);
const activeOrders = computed(() => orders.value.filter(o => o.status !== 'COMPLETED' && o.status !== 'CANCELLED'));
const sorted = computed(() => [...activeOrders.value].sort((a, b) => 
  oldest.value ? new Date(a.createdAt) - new Date(b.createdAt) : new Date(b.createdAt) - new Date(a.createdAt)
));
function label(s) {
  return ({ PENDING_PAYMENT: '카운터 결제대기', PAID: '신규', MAKING: '처리중', READY: '준비완료', COMPLETED: '완료', CANCELLED: '취소' })[s] || s;
}
function formatWaitingNumber(order) {
  const number = order.waitingNumber ?? order.orderId;
  return String(number).padStart(3, '0');
}
function getFlavors(options) {
  if (!options) return [];
  return options.filter(opt => !opt.startsWith('용기:') && !opt.startsWith('드라이아이스:') && !opt.startsWith('스푼:'));
}
function getExtras(options) {
  if (!options) return [];
  return options.filter(opt => opt.startsWith('용기:') || opt.startsWith('드라이아이스:') || opt.startsWith('스푼:'));
}
function openPopup() {
  showPopup.value = !showPopup.value;
  if (showPopup.value && waitTime.value > 0) {
    tempWaitTime.value = waitTime.value;
  } else if (showPopup.value && waitTime.value === 0) {
    tempWaitTime.value = 20; // default selection
  }
}
async function loadStatus() {
  try {
    const status = (await http.get('/status')).data;
    waitTime.value = status.isBusy ? status.estimatedWaitMinutes : 0;
  } catch (e) {
    console.error(e);
  }
}
async function applyWaitTime() {
  await http.patch('/status', { isBusy: true, estimatedWaitMinutes: tempWaitTime.value });
  waitTime.value = tempWaitTime.value;
  showPopup.value = false;
}
async function clearWaitTime() {
  await http.patch('/status', { isBusy: false, estimatedWaitMinutes: null });
  waitTime.value = 0;
  showPopup.value = false;
}
function openCancelModal(order) {
  cancelTarget.value = order;
  cancelReasonText.value = '';
  showCancelModal.value = true;
}
function closeCancelModal() {
  showCancelModal.value = false;
  cancelTarget.value = null;
}
async function toggleDetail(orderId) {
  if (expandedOrderId.value === orderId) {
    expandedOrderId.value = null;
    detailOrder.value = null;
    return;
  }
  
  expandedOrderId.value = orderId;
  detailOrder.value = null;
  try {
    const res = await http.get(`/orders/${orderId}`);
    detailOrder.value = res.data;
  } catch (e) {
    console.error(e);
    alert('상세 정보를 불러오는데 실패했습니다.');
    expandedOrderId.value = null;
  }
}
async function submitCancel() {
  if (!cancelTarget.value) return;
  await changeStatus(cancelTarget.value, 'CANCELLED', cancelReasonText.value);
  closeCancelModal();
}
async function load() {
  try {
    // SSE 'order' 이벤트마다 다시 조회하므로 완료/취소 이력 수백 건은 받지 않고 처리 중 주문만 요청한다.
    orders.value = (await http.get('/orders?activeOnly=true')).data;
  } catch (e) {
    console.error(e);
  }
}
async function changeStatus(o, status, cancelReason = null) {
  await http.patch(`/orders/${o.orderId}/status`, { status, cancelReason });
  // 서버 응답이 성공하면 목록 재조회 전에 버튼과 상태 문구를 즉시 바꿔 체감 지연을 없앤다.
  o.status = status;
  if (status === 'COMPLETED' || status === 'CANCELLED') {
    orders.value = orders.value.filter(order => order.orderId !== o.orderId);
  }
}
// 카운터에서 현금을 받은 뒤 백엔드의 포인트/쿠폰/재고 확정 처리까지 한 번에 실행한다.
async function confirmCash(o) {
  await http.post(`/orders/${o.orderId}/confirm-cash`);
  await load();
}
onMounted(() => {
  load();
  loadStatus();
  unsubscribeOrder = onOrder(load);
});
onBeforeUnmount(() => unsubscribeOrder?.());
</script>
<style scoped>
.shell { min-height: 100vh; color: #202a39; background: #f3f6fa; }
main { margin-left: 238px; padding: 31px 34px; }
header { display: flex; justify-content: space-between; align-items: center; }
h1 { margin: 0; font-size: 26px; }
header p { margin: 7px 0; color: #7c8796; font-size: 11px; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.sort-btn { padding: 10px 15px; border: 1px solid #dce1e8; background: #fff; border-radius: 10px; color: #566173; font-size: 10px; font-weight: 800; cursor: pointer; }
.busy-wrapper { position: relative; }
.busy { display: flex; align-items: center; gap: 6px; padding: 10px 15px; border-radius: 10px; font-size: 10px; font-weight: 800; cursor: pointer; border: 1px solid #ff6c11; background: #fff; color: #ff6c11; transition: all 0.2s; }
.busy.active { background: #ff6c11; color: #fff; }
.busy-popup { position: absolute; top: 110%; right: 0; width: 260px; padding: 20px; background: #fff; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); border: 1px solid #e0e5ec; z-index: 10; }
.busy-popup h3 { margin: 0 0 8px; font-size: 13px; color: #1f2938; }
.busy-popup p { margin: 0 0 16px; font-size: 10px; color: #7c8796; line-height: 1.4; }
.time-options { display: flex; gap: 8px; margin-bottom: 16px; }
.time-options button { flex: 1; padding: 10px 0; border: 1px solid #dce1e8; background: #fff; border-radius: 8px; color: #566173; font-size: 11px; font-weight: bold; cursor: pointer; }
.time-options button.active { border-color: #ff6c11; color: #ff6c11; background: #fff8ef; }
.apply-btn { width: 100%; padding: 12px; background: #ff6c11; color: #fff; border: none; border-radius: 8px; font-size: 11px; font-weight: bold; cursor: pointer; }
.clear-btn { width: 100%; padding: 8px; margin-top: 8px; background: none; color: #7c8796; border: none; font-size: 10px; text-decoration: underline; cursor: pointer; }
section { overflow: hidden; margin-top: 20px; background: #fff; border: 1px solid #e0e5ec; border-radius: 15px; }
table { width: 100%; border-collapse: collapse; }
th { padding: 14px 18px; color: #747f8f; background: #f8fafc; font-size: 10px; text-align: left; }
td { padding: 15px 18px; border-top: 1px solid #edf0f4; font-size: 10px; }
td > b { font-size: 12px; }
.elapsed, .type { padding: 5px 8px; border-radius: 6px; font-weight: 800; }
.danger { color: #ef3948; background: #ffe8eb; }
.warn { color: #d27c00; background: #fff2cf; }
.safe { color: #0a9d56; background: #e4f8ed; }
.type.dine_in { color: #3671e9; background: #eaf1ff; }
.type.takeout { color: #7253e7; background: #f0ebff; }
.s-PAID { color: #6165ee; }
.s-PENDING_PAYMENT { color: #d27c00; }
.cash-badge { display: inline-block; margin-left: 6px; padding: 3px 6px; border-radius: 5px; color: #fff; background: #d27c00; font-size: 8px; }
.s-MAKING { color: #dc7200; }
.s-COMPLETED { color: #0b9c58; }
.s-CANCELLED { color: #a3a9b2; }
.actions { display: flex; gap: 6px; }
.actions button { padding: 7px 10px; border-radius: 7px; font-size: 9px; font-weight: 800; cursor: pointer; }
.start, .complete { color: #0a9c54; border: 0; background: #dff8e9; }
.cash-confirm { color: #fff; border: 0; background: #d27c00; }
.cancel { color: #ef3e4b; border: 1px solid #ffabb1; background: #fff; }
.empty { padding: 60px; text-align: center; color: #9ba3af; }
/* 모달 디자인 */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-content { background: #fff; width: 440px; border-radius: 16px; padding: 28px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); box-sizing: border-box; }
.modal-header { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 24px; }
.icon-circle { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 50%; background: #ffe8eb; color: #ef3948; flex-shrink: 0; }
.modal-title-group h2 { margin: 0 0 6px; font-size: 18px; color: #1f2938; }
.modal-title-group p { margin: 0; font-size: 11px; color: #747f8f; }
.modal-body label { display: block; font-size: 12px; font-weight: bold; color: #1f2938; margin-bottom: 8px; }
.modal-body .required { color: #ef3948; }
.modal-body textarea { width: 100%; height: 90px; padding: 14px; border: 1px solid #dce1e8; border-radius: 8px; font-size: 12px; color: #1f2938; resize: none; margin-bottom: 8px; outline: none; box-sizing: border-box; font-family: inherit; }
.modal-body textarea:focus { border-color: #6165ee; }
.modal-body small { display: block; font-size: 10px; color: #9ba3af; }
.modal-footer { display: flex; gap: 12px; margin-top: 28px; }
.modal-footer button { flex: 1; padding: 14px; border-radius: 8px; font-size: 13px; font-weight: bold; cursor: pointer; }
.btn-close { background: #fff; border: 1px solid #dce1e8; color: #566173; }
.btn-cancel-submit { background: #da291c; border: none; color: #fff; }

.clickable-row { cursor: pointer; transition: background-color 0.2s; }
.clickable-row:hover { background-color: #f8fafc; }
tr.expanded { background-color: #f8fafc; }
tr.expanded td { border-bottom: none; }
.detail-row td { padding: 0; border-top: none; }
.detail-td { padding: 0; }
.detail-container { padding: 20px 34px; background: #fdfdfd; border-bottom: 1px solid #edf0f4; animation: slideDown 0.2s ease-out; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); }
.loading-state { padding: 40px; text-align: center; color: #7c8796; font-size: 13px; border-bottom: 1px solid #edf0f4; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }

.order-items { list-style: none; padding: 0; margin: 0 0 20px 0; max-height: 420px; overflow-y: auto; border-bottom: 1px solid #edf0f4; }
.order-items li { padding: 16px 0; border-bottom: 1px dashed #edf0f4; }
.order-items li:last-child { border-bottom: none; }
.item-main { display: flex; justify-content: space-between; font-size: 13px; font-weight: bold; color: #1f2938; margin-bottom: 8px; }
.item-details { background: #f8fafc; padding: 10px 14px; border-radius: 8px; display: flex; flex-direction: column; gap: 8px; }
.flavors-wrapper, .extras-wrapper { display: flex; flex-direction: column; gap: 6px; }
.detail-label { font-size: 10px; font-weight: bold; color: #747f8f; }
.flavor-tags, .extra-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.flavor-tag { background: #ffe8eb; color: #da291c; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: bold; border: 1px solid #ffd4d8; }
.extra-tag { background: #eaf1ff; color: #3671e9; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: bold; border: 1px solid #d4e2ff; }
.order-summary { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #566173; }
.summary-row { display: flex; justify-content: space-between; }
.summary-row.total { font-size: 15px; font-weight: bold; color: #da291c; margin-top: 10px; padding-top: 12px; border-top: 2px solid #edf0f4; }
.discount { color: #3671e9; }
@media(max-width: 760px) {
  main { margin-left: 0; padding: 20px; }
  section { overflow: auto; }
  table { min-width: 850px; }
}
</style>
