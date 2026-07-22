<template>
  <div class="shell">
    <BranchSidebar active="dashboard" :new-orders="newCount" />
    <main>
      <header>
        <div>
          <h1>대시보드</h1>
          <p>{{ today }} · {{ session.branchName }} 오늘 현황</p>
        </div>
        <div class="branch-pill">
          🔔 <span>{{ branchInitial }}</span> {{ session.branchName }}
        </div>
      </header>

      <div v-if="notices.length" class="notice-wrap" ref="noticeBoxRef">
        <div class="notice" @click="noticesOpen = !noticesOpen">
          <b>{{ activeNotice.noticeType === 'EVENT' ? 'EVENT' : 'NOTICE' }}</b>
          <span>{{ formatShortDate(activeNotice.postedAt) }}</span>
          <strong>{{ activeNotice.title }}</strong>
          <i>{{ notices.length > 1 ? `+${notices.length - 1}` : '' }}</i>
        </div>
        
        <div v-if="noticesOpen" class="notice-list">
          <div 
            v-for="notice in notices" 
            :key="`${notice.noticeType}-${notice.id}`" 
            class="notice-item" 
            @click="goToNotice(notice)"
          >
            <b :class="notice.noticeType === 'EVENT' ? 'tag-event' : 'tag-notice'">
              {{ notice.noticeType === 'EVENT' ? 'EVENT' : 'NOTICE' }}
            </b>
            <div>
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.content }}</p>
            </div>
            <span>{{ formatShortDate(notice.postedAt) }}</span>
          </div>
        </div>
      </div>

      <section class="stats">
        <article>
          <span>신규 주문</span>
          <b>{{ newCount }}</b>
          <small class="orange">즉시 확인 필요</small>
        </article>
        <article>
          <span>처리 중</span>
          <b>{{ makingCount }}</b>
          <small>조리 진행 중</small>
        </article>
        <article>
          <span>오늘 완료</span>
          <b>{{ completedCount }}</b>
          <small class="green">실시간 집계</small>
        </article>
        <article>
          <span>오늘 매출</span>
          <b class="money">₩{{ sales.toLocaleString() }}</b>
          <small class="green">키오스크 결제 기준</small>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="orders-card">
          <div class="card-head">
            <h2>실시간 주문</h2>
            <RouterLink to="/branch/orders">주문 관리 →</RouterLink>
          </div>
          
          <div v-if="!orders.length" class="empty">접수된 주문이 없습니다.</div>
          
          <div v-for="order in recentOrders.slice(0, 4)" :key="order.orderId" class="order-row">
            <span class="ice">🍨</span>
            <div>
              <strong>#{{ order.waitingNumber }} · {{ order.orderType === 'TAKEOUT' ? '포장' : '매장' }}</strong>
              <p>{{ order.menuSummary }}</p>
            </div>
            <em :class="order.status.toLowerCase()">{{ statusLabel(order.status) }}</em>
            <small>{{ order.elapsedMinutes }}분 전</small>
          </div>
        </article>

        <div class="right-column">
          <article>
            <h2>재고 부족 알림</h2>
            <div v-for="item in lowItems.slice(0, 3)" :key="item.flavorId" class="stock-row">
              <i :class="item.remainingGrams === 0 ? 'red' : 'amber'"></i>
              <strong>{{ item.flavorName }}</strong>
              <span>{{ item.remainingGrams === 0 ? '품절' : `${item.remainingGrams.toLocaleString()}g` }}</span>
            </div>
          </article>
          
          <article>
            <h2>재고 배송 현황</h2>
            <div v-for="item in shippingItems.slice(0, 3)" :key="item.flavorId" class="delivery">
              <span>🍨</span>
              <strong>{{ item.flavorName }} 1개 · 3일 내 도착 예정</strong>
              <b>{{ item.requestStatus }}</b>
            </div>
            <p v-if="!shippingItems.length" class="empty">진행 중인 자동 발주가 없습니다.</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import http from '../../api/branch';
import BranchSidebar from '../../components/branch/BranchSidebar.vue';

const router = useRouter();
const session = JSON.parse(localStorage.getItem('branch-session') || '{"branchName":"지점"}');
const branchInitial = session.branchName.slice(0, 1);

const orders = ref([]);
const inventory = ref([]);
const notices = ref([]);

const today = new Date().toLocaleDateString('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
});

let timer;

function goToNotice(notice) {
  router.push(`/branch/notices/${notice.noticeType}/${notice.id}`);
}

const noticesOpen = ref(false);
const noticeBoxRef = ref(null);
const activeNoticeIndex = ref(0);

const activeNotice = computed(() => 
  notices.value.length ? notices.value[activeNoticeIndex.value % notices.value.length] : null
);

let rotateTimer = null;

function startRotation() {
  if (rotateTimer) clearInterval(rotateTimer);
  rotateTimer = setInterval(() => {
    if (noticesOpen.value || notices.value.length <= 1) return;
    activeNoticeIndex.value = (activeNoticeIndex.value + 1) % notices.value.length;
  }, 5000);
}

function handleOutsideClick(e) {
  if (noticeBoxRef.value && !noticeBoxRef.value.contains(e.target)) {
    noticesOpen.value = false;
  }
}

function formatShortDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { 
    month: '2-digit', 
    day: '2-digit' 
  }).format(new Date(value));
}

const todayOrders = computed(() => 
  orders.value.filter(o => new Date(o.createdAt).toDateString() === new Date().toDateString())
);

const newCount = computed(() => todayOrders.value.filter(o => o.status === 'PAID').length);

const makingCount = computed(() => 
  todayOrders.value.filter(o => ['MAKING', 'READY'].includes(o.status)).length
);

const completedCount = computed(() => 
  todayOrders.value.filter(o => o.status === 'COMPLETED').length
);

const sales = computed(() => 
  todayOrders.value
    .filter(o => o.status !== 'CANCELLED')
    .reduce((s, o) => s + o.finalAmount, 0)
);

const lowItems = computed(() => 
  inventory.value
    .filter(i => i.remainingGrams <= 3000)
    .sort((a, b) => a.remainingGrams - b.remainingGrams)
);

const shippingItems = computed(() => inventory.value.filter(i => i.requestStatus));

const recentOrders = computed(() => 
  [...orders.value]
    .filter(o => o.status !== 'COMPLETED' && o.status !== 'CANCELLED')
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
);

function statusLabel(s) {
  return ({
    PAID: '신규',
    MAKING: '처리중',
    READY: '준비완료',
    COMPLETED: '완료',
    CANCELLED: '취소'
  })[s] || s;
}

async function load() {
  try {
    const [o, i, n] = await Promise.all([
      http.get('/orders'),
      http.get('/inventory'),
      http.get('/notices')
    ]);
    orders.value = o.data;
    inventory.value = i.data;
    notices.value = n.data;
  } catch (e) {
    console.error(e);
  }
}

onMounted(() => {
  load();
  timer = setInterval(load, 3000);
  startRotation();
  document.addEventListener('click', handleOutsideClick);
});

onBeforeUnmount(() => {
  clearInterval(timer);
  clearInterval(rotateTimer);
  document.removeEventListener('click', handleOutsideClick);
});
</script>

<style scoped>
.shell {
  min-height: 100vh;
  color: #1f2938;
  background: #f3f6fa;
}

main {
  margin-left: 238px;
  padding: 32px 34px 55px;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h1 {
  margin: 0;
  font-size: 27px;
}

header p {
  margin: 7px 0 0;
  color: #7f8997;
  font-size: 11px;
}

.branch-pill {
  padding: 10px 13px;
  background: #fff;
  border: 1px solid #dfe4eb;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 800;
}

.branch-pill span {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #fff;
  background: #6568ee;
  border-radius: 50%;
}

.notice-wrap {
  position: relative;
  margin: 24px 0;
}

.notice {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border: 1px solid #dcdfff;
  background: linear-gradient(90deg, #ffeaf5, #eff1ff);
  border-radius: 14px;
  font-size: 11px;
  cursor: pointer;
}

.notice b {
  padding: 5px 9px;
  color: #fff;
  background: #686bf0;
  border-radius: 5px;
  letter-spacing: .08em;
}

.notice span {
  color: #7478ef;
}

.notice i {
  margin-left: auto;
  color: #9ba0ef;
}

.notice-list {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  max-height: 340px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e5e9ef;
  border-radius: 14px;
  box-shadow: 0 12px 30px rgb(34 48 68 / 12%);
}

.notice-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 18px;
  border-bottom: 1px solid #f1f3f7;
  cursor: pointer;
}

.notice-item:hover {
  background: #f8fafc;
}

.notice-item:last-child {
  border: 0;
}

.notice-item strong {
  display: block;
  font-size: 12px;
}

.notice-item p {
  overflow: hidden;
  margin: 4px 0 0;
  color: #8c95a2;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item span {
  flex-shrink: 0;
  color: #9ba0ef;
  font-size: 9px;
}

.tag-event {
  flex-shrink: 0;
  padding: 5px 9px;
  color: #fff;
  background: #f2a300;
  border-radius: 5px;
  font-size: 9px;
  letter-spacing: .08em;
}

.tag-notice {
  flex-shrink: 0;
  padding: 5px 9px;
  color: #fff;
  background: #686bf0;
  border-radius: 5px;
  font-size: 9px;
  letter-spacing: .08em;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stats article, 
.dashboard-grid article {
  background: #fff;
  border: 1px solid #e5e9ef;
  border-radius: 16px;
  box-shadow: 0 3px 8px rgb(34 48 68 / 3%);
}

.stats article {
  display: grid;
  padding: 20px;
}

.stats span {
  color: #747e8d;
  font-size: 11px;
}

.stats b {
  margin: 17px 0 7px;
  font-size: 28px;
}

.stats .money {
  font-size: 20px;
}

.stats small {
  font-size: 10px;
}

.orange {
  color: #ff8700;
}

.green {
  color: #00a84f;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.75fr 1fr;
  gap: 17px;
  margin-top: 20px;
}

.dashboard-grid article {
  padding: 20px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-head a {
  color: #5d62ef;
  font-size: 10px;
  font-weight: 800;
  text-decoration: none;
}

h2 {
  margin: 0 0 15px;
  font-size: 15px;
}

.order-row {
  display: grid;
  grid-template-columns: 42px 1fr auto 52px;
  gap: 10px;
  align-items: center;
  padding: 11px;
  background: #f8fafc;
  border-radius: 10px;
  margin-top: 9px;
}

.ice {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  background: #fff0f6;
  border-radius: 50%;
}

.order-row strong {
  font-size: 11px;
}

.order-row p {
  overflow: hidden;
  margin: 4px 0 0;
  color: #737e8e;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-row em {
  padding: 5px 8px;
  border-radius: 6px;
  font-size: 9px;
  font-style: normal;
}

.order-row em.paid {
  color: #5960ec;
  background: #eceeff;
}

.order-row em.making {
  color: #c97900;
  background: #fff1c9;
}

.order-row em.completed {
  color: #079354;
  background: #ddf8e9;
}

.order-row small {
  color: #9ba3af;
  font-size: 9px;
}

.right-column {
  display: grid;
  gap: 17px;
}

.stock-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 0;
}

.stock-row i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.red {
  background: #ef4454;
}

.amber {
  background: #f5a000;
}

.stock-row strong {
  font-size: 10px;
}

.stock-row span {
  margin-left: auto;
  color: #ef4454;
  font-size: 10px;
}

.delivery {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 9px;
  background: #f8fafc;
  border-radius: 9px;
}

.delivery strong {
  font-size: 9px;
}

.delivery b {
  margin-left: auto;
  padding: 5px;
  color: #5f65ed;
  background: #eaeefe;
  border-radius: 6px;
  font-size: 8px;
}

.empty {
  padding: 20px;
  color: #9aa3af;
  text-align: center;
  font-size: 10px;
}

@media(max-width: 900px) {
  .stats {
    grid-template-columns: 1fr 1fr;
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media(max-width: 760px) {
  main {
    margin-left: 0;
    padding: 20px;
  }
  .stats {
    grid-template-columns: 1fr 1fr;
  }
}
</style>