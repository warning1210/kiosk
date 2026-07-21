<template>
  <main class="page">
    <AdminSidebar active="coupons" />

    <section class="content">
      <AdminPageHeader title="쿠폰 발송" subtitle="본사에서 쿠폰을 생성해 대상 지점 및 고객에게 발송을 관리할 수 있습니다." />

      <div class="summary">
        <AdminStatCard icon="🎫" label="전체 발급" :value="`${coupons.length}개`" />
        <AdminStatCard icon="✓" label="사용 완료" :value="`${usedCount}개`" tone="green" />
        <AdminStatCard icon="📈" label="사용률" :value="usageRate" tone="blue" />
        <AdminStatCard icon="⚠" label="만료 예정" :value="`${expiringSoonCount}개`" :delta="`${EXPIRING_SOON_DAYS}일 이내`" tone="orange" />
      </div>

      <section class="invite-card">
        <h2>등급별 쿠폰 발급</h2>
        <p class="section-desc">선택한 등급의 고객 전원에게 쿠폰을 1장씩 발급합니다. 발송(알림/문자)은 별도이며, 아래 목록의 코드를 직접 전달해주세요.</p>
        <form class="event-form" @submit.prevent="issueCoupon">
          <input v-model.trim="couponForm.couponName" required placeholder="쿠폰 이름">
          <select v-model="couponForm.grade" required>
            <option value="" disabled>대상 등급</option>
            <option v-for="grade in grades" :key="grade" :value="grade">{{ gradeLabel(grade) }}</option>
          </select>
          <input v-model.number="couponForm.discountAmount" type="number" min="1" required placeholder="할인 금액(원)">
          <input v-model="couponForm.expiresAt" type="datetime-local" required>
          <select v-model="couponForm.eventId">
            <option value="">연결 이벤트 없음</option>
            <option v-for="event in events" :key="event.eventId" :value="event.eventId">{{ event.eventName }}</option>
          </select>
          <button :disabled="issuingCoupon" type="submit">{{ issuingCoupon ? '발급 중' : '쿠폰 발급' }}</button>
        </form>
        <p v-if="couponFormError" class="alert">{{ couponFormError }}</p>
        <p v-if="couponIssuedMessage" class="issued">{{ couponIssuedMessage }}</p>
      </section>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>발급된 쿠폰</h2>
            <span>{{ couponsLoading ? '불러오는 중' : `${filteredCoupons.length}건` }}</span>
          </div>
          <label class="search"><span>⌕</span><input v-model="keyword" placeholder="쿠폰명, 코드 검색"></label>
        </div>

        <div class="tabs">
          <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
            {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!couponsLoading && !pagedCoupons.length" class="empty">발급된 쿠폰이 없습니다.</div>
        <table v-else>
          <thead><tr><th>쿠폰명</th><th>대상</th><th>코드</th><th>할인금액</th><th>상태</th><th>만료일</th></tr></thead>
          <tbody>
            <tr v-for="coupon in pagedCoupons" :key="coupon.couponId">
              <td><strong>{{ coupon.couponName }}</strong></td>
              <td>{{ coupon.customerMobileNumber || '-' }}<small>{{ gradeLabel(coupon.customerGrade) }}</small></td>
              <td><code class="coupon-code">{{ coupon.qrToken }}</code></td>
              <td>₩{{ coupon.discountAmount.toLocaleString() }}</td>
              <td><span :class="['status', coupon.couponStatus === 'AVAILABLE' ? 'approved' : coupon.couponStatus === 'USED' ? 'used' : 'expired']">{{ couponStatusLabel(coupon.couponStatus) }}</span></td>
              <td>{{ formatDate(coupon.expiresAt) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredCoupons.length }}건 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredCoupons.length" :page-size="pageSize" />
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

const grades = ['FRIEND', 'FAMILY', 'VIP']

const events = ref([])
const coupons = ref([])
const couponsLoading = ref(true)
const issuingCoupon = ref(false)
const couponFormError = ref('')
const couponIssuedMessage = ref('')
const couponForm = reactive({ couponName: '', grade: '', discountAmount: null, expiresAt: '', eventId: '' })
const keyword = ref('')
const filter = ref('all')
const page = ref(1)
const pageSize = 6

const tabs = [
  { label: '전체', value: 'all' }, { label: '사용 가능', value: 'AVAILABLE' },
  { label: '사용 완료', value: 'USED' }, { label: '만료', value: 'EXPIRED' }
]

onMounted(() => {
  loadEvents()
  loadCoupons()
})

async function loadEvents() {
  try {
    events.value = (await http.get('/hq/events')).data
  } catch {
    events.value = []
  }
}

async function loadCoupons() {
  couponsLoading.value = true
  try {
    coupons.value = (await http.get('/hq/coupons')).data
  } catch (e) {
    couponFormError.value = e.response?.data?.message || '쿠폰 목록을 불러오지 못했습니다.'
  } finally {
    couponsLoading.value = false
  }
}

async function issueCoupon() {
  issuingCoupon.value = true
  couponFormError.value = ''
  couponIssuedMessage.value = ''
  try {
    const { data } = await http.post('/hq/coupons', {
      ...couponForm,
      eventId: couponForm.eventId || null
    })
    coupons.value = [...data, ...coupons.value]
    couponIssuedMessage.value = `${data.length}명에게 쿠폰을 발급했습니다.`
    Object.assign(couponForm, { couponName: '', grade: '', discountAmount: null, expiresAt: '', eventId: '' })
  } catch (e) {
    couponFormError.value = e.response?.data?.message || '쿠폰을 발급하지 못했습니다.'
  } finally {
    issuingCoupon.value = false
  }
}

const EXPIRING_SOON_DAYS = 7

const usedCount = computed(() => coupons.value.filter(c => c.couponStatus === 'USED').length)
const expiringSoonCount = computed(() => {
  const now = Date.now()
  const soon = now + EXPIRING_SOON_DAYS * 86400000
  return coupons.value.filter(c => c.couponStatus === 'AVAILABLE' && new Date(c.expiresAt).getTime() <= soon).length
})
const usageRate = computed(() => coupons.value.length ? `${Math.round(usedCount.value / coupons.value.length * 100)}%` : '0%')

function tabCount(value) {
  if (value === 'all') return coupons.value.length
  return coupons.value.filter(c => c.couponStatus === value).length
}

const filteredCoupons = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return coupons.value.filter(c =>
    (filter.value === 'all' || c.couponStatus === filter.value) &&
    (!word || c.couponName.toLowerCase().includes(word) || c.qrToken.toLowerCase().includes(word))
  )
})
const pageStart = computed(() => filteredCoupons.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredCoupons.value.length))
const pagedCoupons = computed(() => filteredCoupons.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function gradeLabel(grade) {
  return { FRIEND: '프렌드', FAMILY: '패밀리', VIP: 'VIP' }[grade] || grade
}
function couponStatusLabel(status) {
  return { AVAILABLE: '사용 가능', USED: '사용됨', EXPIRED: '만료' }[status] || status
}
function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.invite-card{margin-bottom:20px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.invite-card h2{margin:0 0 12px;font-size:14px}
.section-desc{margin:0 0 12px;color:#8c95a2;font-size:11px}
.event-form{display:grid;grid-template-columns:1.4fr 1fr 1fr;gap:8px}.event-form input,.event-form select{padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}.event-form button{grid-column:1/-1;padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}.event-form button:disabled{opacity:.55}
.alert{margin-top:10px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.issued{margin-top:10px;padding:13px;color:#0b9654;background:#e2f8ec;border:1px solid #c7f0da;border-radius:9px;font-size:11px}
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
td small{display:block;margin-top:3px;color:#98a1ae;font-size:9px}
.coupon-code{padding:6px 8px;color:#5d62e8;background:#f0f1ff;border-radius:6px;font-size:9px}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.approved{color:#0b9654;background:#e2f8ec}.status.used{color:#3169c7;background:#e4f0ff}.status.expired{color:#c63750;background:#ffe8ed}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.event-form{grid-template-columns:1fr}table{display:block;overflow-x:auto}}
</style>
