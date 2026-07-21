<template>
  <main class="page">
    <AdminSidebar active="events" />

    <section class="content">
      <AdminPageHeader title="이벤트 관리" subtitle="본사 이벤트를 생성하고 지점, 기간, 참여 현황을 관리할 수 있습니다." />

      <div class="summary">
        <AdminStatCard icon="🎁" label="진행 중 이벤트" :value="`${liveCount}건`" tone="pink" />
        <AdminStatCard icon="🕒" label="예정 이벤트" :value="`${scheduledCount}건`" tone="blue" />
        <AdminStatCard icon="✓" label="종료 이벤트" :value="`${endedCount}건`" tone="green" />
        <AdminStatCard icon="▦" label="전체 이벤트" :value="`${events.length}건`" />
      </div>

      <section class="invite-card">
        <h2>새 이벤트 만들기</h2>
        <form class="event-form" @submit.prevent="createEvent">
          <input v-model.trim="form.eventName" required placeholder="이벤트 이름">
          <select v-model="form.eventType" required>
            <option value="" disabled>이벤트 유형</option>
            <option v-for="type in eventTypes" :key="type" :value="type">{{ eventTypeLabel(type) }}</option>
          </select>
          <template v-if="form.eventType === 'FLAVOR_DISCOUNT'">
            <select v-model="form.benefitType">
              <option value="DISCOUNT_AMOUNT">정액 할인(원)</option>
              <option value="DISCOUNT_RATE">정률 할인(%)</option>
            </select>
            <input
              v-if="form.benefitType === 'DISCOUNT_RATE'"
              v-model.number="form.discountRate"
              type="number" min="1" max="100" required placeholder="할인율(%)"
            >
            <input
              v-else
              v-model.number="form.discountAmount"
              type="number" min="1" required placeholder="할인 금액(원)"
            >
          </template>
          <input v-model="form.startAt" type="date" required>
          <input v-model="form.endAt" type="date" required>
          <textarea v-model.trim="form.description" placeholder="설명 (지점 공지사항에 그대로 노출됩니다)" rows="2"></textarea>
          <button :disabled="creating" type="submit">{{ creating ? '생성 중' : '이벤트 생성' }}</button>
        </form>
        <p v-if="form.eventType === 'COUPON'" class="hint">쿠폰 발급 자체는 <RouterLink to="/admin/coupons">쿠폰 발송</RouterLink> 탭에서 진행하세요. 여기서는 캠페인 이름/기간만 등록합니다.</p>
        <p v-if="form.eventType === 'FLAVOR_DISCOUNT'" class="hint">실제로 어느 맛에 할인을 붙일지는 각 지점이 지점의 "이벤트 관리" 화면에서 선택합니다.</p>
        <p v-if="formError" class="alert">{{ formError }}</p>
      </section>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>이벤트 목록</h2>
            <span>{{ loading ? '불러오는 중' : `${filteredEvents.length}건` }}</span>
          </div>
          <label class="search"><span>⌕</span><input v-model="keyword" placeholder="이벤트명 검색"></label>
        </div>

        <div class="tabs">
          <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
            {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!loading && !pagedEvents.length" class="empty">등록된 이벤트가 없습니다.</div>
        <table v-else>
          <thead><tr><th>이벤트명</th><th>유형</th><th>할인</th><th>기간</th><th>상태</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="event in pagedEvents" :key="event.eventId">
              <td><strong>{{ event.eventName }}</strong><small>{{ event.description || '설명 없음' }}</small></td>
              <td><span class="tag">{{ eventTypeLabel(event.eventType) }}</span></td>
              <td>{{ discountLabel(event) }}</td>
              <td>{{ formatDate(event.startAt) }} ~ {{ formatEndDate(event.endAt) }}</td>
              <td><span :class="['status', bucketOf(event)]">{{ bucketLabel(event) }}</span></td>
              <td><button class="detail" type="button">상세보기</button></td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredEvents.length }}건 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredEvents.length" :page-size="pageSize" />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

// 이벤트는 크게 2가지뿐: 본점이 쿠폰을 배부하는 것(COUPON) / 특정 상품(맛)에 할인을 붙이는 것(FLAVOR_DISCOUNT).
// 후자는 할인 값(율 또는 금액)만 본점이 정하고, 실제로 어느 맛에 붙일지는 지점이 고른다(event_branch_flavor).
const eventTypes = ['COUPON', 'FLAVOR_DISCOUNT']

const events = ref([])
const loading = ref(true)
const creating = ref(false)
const formError = ref('')
const form = reactive({
  eventName: '', eventType: '', benefitType: 'DISCOUNT_AMOUNT',
  discountRate: null, discountAmount: null, startAt: '', endAt: '', description: ''
})
const keyword = ref('')
const filter = ref('all')
const page = ref(1)
const pageSize = 6

const tabs = [
  { label: '전체', value: 'all' }, { label: '진행중', value: 'live' },
  { label: '예정', value: 'scheduled' }, { label: '종료', value: 'ended' }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    events.value = (await http.get('/hq/events')).data
  } catch (e) {
    formError.value = e.response?.data?.message || '이벤트 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 날짜만 입력받되, 시간 기준은 항상 자정(00:00)이다. 종료일은 그 날짜까지 포함되도록
// "종료일 다음날 자정"을 실제 endAt으로 보낸다 - 그래야 시작일=종료일(하루짜리 이벤트)도 만들 수 있다.
function toStartOfDay(dateStr) {
  return `${dateStr}T00:00:00`
}
function toExclusiveEndOfDay(dateStr) {
  const next = new Date(`${dateStr}T00:00:00`)
  next.setDate(next.getDate() + 1)
  const pad = (n) => String(n).padStart(2, '0')
  return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}T00:00:00`
}

async function createEvent() {
  creating.value = true
  formError.value = ''
  try {
    const { data } = await http.post('/hq/events', {
      ...form,
      startAt: toStartOfDay(form.startAt),
      endAt: toExclusiveEndOfDay(form.endAt)
    })
    events.value.unshift(data)
    Object.assign(form, {
      eventName: '', eventType: '', benefitType: 'DISCOUNT_AMOUNT',
      discountRate: null, discountAmount: null, startAt: '', endAt: '', description: ''
    })
  } catch (e) {
    formError.value = e.response?.data?.message || '이벤트를 만들지 못했습니다.'
  } finally {
    creating.value = false
  }
}

function bucketOf(event) {
  const now = new Date()
  if (event.status !== 'ACTIVE' && event.status !== 'SCHEDULED') return 'ended'
  if (now < new Date(event.startAt)) return 'scheduled'
  if (now > new Date(event.endAt)) return 'ended'
  return 'live'
}
function bucketLabel(event) {
  return { live: '진행중', scheduled: '예정', ended: '종료' }[bucketOf(event)]
}

const liveCount = computed(() => events.value.filter(e => bucketOf(e) === 'live').length)
const scheduledCount = computed(() => events.value.filter(e => bucketOf(e) === 'scheduled').length)
const endedCount = computed(() => events.value.filter(e => bucketOf(e) === 'ended').length)

function tabCount(value) {
  if (value === 'all') return events.value.length
  return events.value.filter(e => bucketOf(e) === value).length
}

const filteredEvents = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return events.value.filter(e => (filter.value === 'all' || bucketOf(e) === filter.value) && (!word || e.eventName.toLowerCase().includes(word)))
})
const pageStart = computed(() => filteredEvents.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredEvents.value.length))
const pagedEvents = computed(() => filteredEvents.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function eventTypeLabel(type) {
  return { COUPON: '쿠폰 배부', FLAVOR_DISCOUNT: '상품(맛) 할인' }[type] || type
}
function discountLabel(event) {
  if (event.eventType !== 'FLAVOR_DISCOUNT') return '-'
  return event.benefitType === 'DISCOUNT_RATE' ? `${event.discountRate}%` : `₩${(event.discountAmount ?? 0).toLocaleString()}`
}
function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value))
}
// endAt은 "종료일 다음날 자정"으로 저장돼 있어서, 화면엔 관리자가 실제로 고른 종료일(하루 전)로 보여준다
function formatEndDate(value) {
  const date = new Date(value)
  date.setDate(date.getDate() - 1)
  return formatDate(date)
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.invite-card{margin-bottom:20px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.invite-card h2{margin:0 0 12px;font-size:14px}
.event-form{display:grid;grid-template-columns:1.4fr 1fr 1fr;gap:8px}.event-form textarea{grid-column:1/-1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px;font-family:inherit;resize:vertical}.event-form input,.event-form select{padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}.event-form button{grid-column:1/-1;padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}.event-form button:disabled{opacity:.55}
.alert{margin-top:10px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.hint{margin-top:10px;color:#8c95a2;font-size:11px}.hint a{color:#5960e9;font-weight:800;text-decoration:none}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.tabs{display:flex;gap:3px;padding:12px 22px 0}
.tabs button{padding:8px 11px;color:#697487;border:0;background:transparent;border-radius:8px;font-size:12px;font-weight:700;cursor:pointer}
.tabs button span{padding:2px 5px;background:#eef0f3;border-radius:6px;font-size:9px}
.tabs button.active{color:#5f63ee;background:#eef0ff}.tabs button.active span{background:#dbdefc}
table{width:100%;border-collapse:collapse;font-size:11px;margin-top:8px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
td strong{display:block;font-size:12px}td small{display:block;margin-top:3px;color:#98a1ae;font-size:9px}
.tag{padding:5px 8px;color:#5d62e8;background:#f0f1ff;border-radius:6px;font-size:9px;font-weight:800}
.status{padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}.status.live{color:#0b9654;background:#e2f8ec}.status.scheduled{color:#d57d00;background:#fff3d6}.status.ended{color:#697487;background:#eef0f3}
.detail{padding:7px 10px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.event-form{grid-template-columns:1fr}table{display:block;overflow-x:auto}}
</style>
