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
          <select v-model="couponForm.discountType">
            <option value="AMOUNT">정액 할인(원)</option>
            <option value="RATE">정률 할인(%)</option>
          </select>
          <input
            v-if="couponForm.discountType === 'RATE'"
            v-model.number="couponForm.discountRate"
            type="number" min="1" max="100" required placeholder="할인율(%)"
          >
          <input
            v-else
            v-model.number="couponForm.discountAmount"
            type="number" min="1" required placeholder="할인 금액(원)"
          >
          <input v-model="couponForm.expiresAt" type="date" required>
          <button :disabled="issuingCoupon" type="submit">{{ issuingCoupon ? '발급 중' : '쿠폰 발급' }}</button>
        </form>
        <p v-if="couponFormError" class="alert">{{ couponFormError }}</p>
        <p v-if="couponIssuedMessage" class="issued">{{ couponIssuedMessage }}</p>
      </section>

      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>발급된 쿠폰</h2>
            <span>{{ couponsLoading ? '불러오는 중' : `${filteredCoupons.length}건 · ${groupedCoupons.length}종` }}</span>
          </div>
          <label class="search"><span>⌕</span><input v-model="keyword" placeholder="쿠폰명 검색"></label>
        </div>

        <div class="tabs">
          <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
            {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!couponsLoading && !pagedGroups.length" class="empty">발급된 쿠폰이 없습니다.</div>
        <table v-else>
          <thead><tr><th>쿠폰명</th><th>할인</th><th>발급 인원</th><th>사용률</th><th>만료일</th></tr></thead>
          <tbody>
            <tr v-for="group in pagedGroups" :key="group.couponName">
              <td><strong>{{ group.couponName }}</strong></td>
              <td>{{ discountLabel(group) }}</td>
              <td>{{ group.total }}명</td>
              <td>{{ usageRateOf(group) }}</td>
              <td>{{ formatExpiresAt(group.expiresAt) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ groupedCoupons.length }}종 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="groupedCoupons.length" :page-size="pageSize" />
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

const grades = ['ALL', 'FRIEND', 'FAMILY', 'VIP']

const coupons = ref([])
const couponsLoading = ref(true)
const issuingCoupon = ref(false)
const couponFormError = ref('')
const couponIssuedMessage = ref('')
const couponForm = reactive({ couponName: '', grade: '', discountType: 'AMOUNT', discountRate: null, discountAmount: null, expiresAt: '' })
const keyword = ref('')
const filter = ref('all')
const page = ref(1)
const pageSize = 6

const tabs = [
  { label: '전체', value: 'all' }, { label: '사용 가능', value: 'AVAILABLE' },
  { label: '만료', value: 'EXPIRED' }
]

onMounted(() => {
  loadCoupons()
})

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

// 날짜만 입력받되, 시간 기준은 항상 자정(00:00)이다. 고른 날짜까지 포함되도록
// "고른 날짜 다음날 자정"을 실제 만료 시각으로 보낸다 (이벤트 기간과 동일한 규칙).
function toExclusiveEndOfDay(dateStr) {
  const next = new Date(`${dateStr}T00:00:00`)
  next.setDate(next.getDate() + 1)
  const pad = (n) => String(n).padStart(2, '0')
  return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}T00:00:00`
}

async function issueCoupon() {
  issuingCoupon.value = true
  couponFormError.value = ''
  couponIssuedMessage.value = ''
  try {
    const { data } = await http.post('/hq/coupons', {
      ...couponForm,
      expiresAt: toExclusiveEndOfDay(couponForm.expiresAt)
    })
    coupons.value = [...data, ...coupons.value]
    couponIssuedMessage.value = `${data.length}명에게 쿠폰을 발급했습니다.`
    Object.assign(couponForm, { couponName: '', grade: '', discountType: 'AMOUNT', discountRate: null, discountAmount: null, expiresAt: '' })
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
// 같은 이름으로 대량 발급하면 수백~수천 건이 개별로 나열돼 보기 힘드므로, 쿠폰명을 하나의
// 캠페인 단위로 인식해서 발급인원/사용률만 요약해서 보여준다 - 개별 발급분은 화면에 노출하지 않는다.
const groupedCoupons = computed(() => {
  const groups = new Map()
  for (const coupon of filteredCoupons.value) {
    if (!groups.has(coupon.couponName)) {
      groups.set(coupon.couponName, {
        couponName: coupon.couponName,
        discountType: coupon.discountType,
        discountRate: coupon.discountRate,
        discountAmount: coupon.discountAmount,
        expiresAt: coupon.expiresAt,
        total: 0,
        used: 0
      })
    }
    const group = groups.get(coupon.couponName)
    group.total++
    if (coupon.couponStatus === 'USED') group.used++
  }
  return Array.from(groups.values())
})

function usageRateOf(group) {
  return group.total ? `${Math.round(group.used / group.total * 100)}%` : '0%'
}

const pageStart = computed(() => groupedCoupons.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, groupedCoupons.value.length))
const pagedGroups = computed(() => groupedCoupons.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function gradeLabel(grade) {
  return { ALL: '전체', FRIEND: '프렌드', FAMILY: '패밀리', VIP: 'VIP' }[grade] || grade
}
function discountLabel(coupon) {
  return coupon.discountType === 'RATE' ? `${coupon.discountRate}%` : `₩${(coupon.discountAmount ?? 0).toLocaleString()}`
}
function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value))
}
// expiresAt은 "고른 날짜 다음날 자정"으로 저장돼 있어서, 화면엔 실제 고른 만료일(하루 전)로 보여준다
function formatExpiresAt(value) {
  const date = new Date(value)
  date.setDate(date.getDate() - 1)
  return formatDate(date)
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
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.event-form{grid-template-columns:1fr}table{display:block;overflow-x:auto}}
</style>
