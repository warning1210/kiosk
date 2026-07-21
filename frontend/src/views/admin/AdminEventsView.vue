<template>
  <main class="page">
    <AdminSidebar active="events" />

    <section class="content">
      <header>
        <div>
          <p>본점 관리</p>
          <h1>이벤트 관리</h1>
          <span>이벤트를 만들면 지정한 기간 동안 지점 대시보드 공지사항에 자동으로 노출됩니다.</span>
        </div>
        <button class="refresh" type="button" @click="load">새로고침</button>
      </header>

      <section class="invite-card">
        <h2>새 이벤트 만들기</h2>
        <form class="event-form" @submit.prevent="createEvent">
          <input v-model.trim="form.eventName" required placeholder="이벤트 이름">
          <select v-model="form.eventType" required>
            <option value="" disabled>이벤트 유형</option>
            <option v-for="type in eventTypes" :key="type" :value="type">{{ eventTypeLabel(type) }}</option>
          </select>
          <select v-model="form.benefitType">
            <option v-for="type in benefitTypes" :key="type" :value="type">{{ benefitTypeLabel(type) }}</option>
          </select>
          <input v-model="form.startAt" type="datetime-local" required>
          <input v-model="form.endAt" type="datetime-local" required>
          <textarea v-model.trim="form.description" placeholder="설명 (지점 공지사항에 그대로 노출됩니다)" rows="2"></textarea>
          <button :disabled="creating" type="submit">{{ creating ? '생성 중' : '이벤트 생성' }}</button>
        </form>
        <p v-if="formError" class="alert">{{ formError }}</p>
      </section>

      <section class="list-card">
        <div class="list-head">
          <h2>이벤트 목록</h2>
          <span>{{ loading ? '불러오는 중' : `${events.length}건` }}</span>
        </div>

        <div v-if="!loading && !events.length" class="empty">등록된 이벤트가 없습니다.</div>
        <article v-for="event in events" :key="event.eventId" class="event-row">
          <div>
            <strong>{{ event.eventName }}</strong>
            <p>{{ event.description || '설명 없음' }}</p>
          </div>
          <span class="tag">{{ eventTypeLabel(event.eventType) }}</span>
          <span :class="['status', isLive(event) ? 'approved' : 'pending']">{{ isLive(event) ? '노출중' : statusLabel(event.status) }}</span>
          <small>{{ formatDate(event.startAt) }} ~ {{ formatDate(event.endAt) }}</small>
        </article>
      </section>

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
          <h2>발급된 쿠폰</h2>
          <span>{{ couponsLoading ? '불러오는 중' : `${coupons.length}건` }}</span>
        </div>

        <div v-if="!couponsLoading && !coupons.length" class="empty">발급된 쿠폰이 없습니다.</div>
        <article v-for="coupon in coupons" :key="coupon.couponId" class="coupon-row">
          <div>
            <strong>{{ coupon.couponName }}</strong>
            <p>{{ coupon.customerMobileNumber || '-' }} · {{ gradeLabel(coupon.customerGrade) }}</p>
          </div>
          <code class="coupon-code">{{ coupon.qrToken }}</code>
          <span>₩{{ coupon.discountAmount.toLocaleString() }}</span>
          <span :class="['status', coupon.couponStatus === 'AVAILABLE' ? 'approved' : 'pending']">{{ couponStatusLabel(coupon.couponStatus) }}</span>
          <small>~{{ formatDate(coupon.expiresAt) }}</small>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'

const eventTypes = ['MONTHLY_FLAVOR', 'SIZE_UP', 'AMOUNT_DISCOUNT', 'COUPON', 'NEW_PRODUCT', 'STOCK_CLEARANCE']
const benefitTypes = ['NONE', 'DISCOUNT_RATE', 'DISCOUNT_AMOUNT', 'SIZE_UP', 'COUPON']
const grades = ['FRIEND', 'FAMILY', 'VIP']

const events = ref([])
const loading = ref(true)
const creating = ref(false)
const formError = ref('')
const form = reactive({ eventName: '', eventType: '', benefitType: 'NONE', startAt: '', endAt: '', description: '' })

const coupons = ref([])
const couponsLoading = ref(true)
const issuingCoupon = ref(false)
const couponFormError = ref('')
const couponIssuedMessage = ref('')
const couponForm = reactive({ couponName: '', grade: '', discountAmount: null, expiresAt: '', eventId: '' })

onMounted(() => {
  load()
  loadCoupons()
})

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

function gradeLabel(grade) {
  return { FRIEND: '프렌드', FAMILY: '패밀리', VIP: 'VIP' }[grade] || grade
}
function couponStatusLabel(status) {
  return { AVAILABLE: '사용 가능', USED: '사용됨', EXPIRED: '만료' }[status] || status
}

async function createEvent() {
  creating.value = true
  formError.value = ''
  try {
    const { data } = await http.post('/hq/events', { ...form })
    events.value.unshift(data)
    Object.assign(form, { eventName: '', eventType: '', benefitType: 'NONE', startAt: '', endAt: '', description: '' })
  } catch (e) {
    formError.value = e.response?.data?.message || '이벤트를 만들지 못했습니다.'
  } finally {
    creating.value = false
  }
}

function isLive(event) {
  const now = new Date()
  return event.status === 'ACTIVE' && new Date(event.startAt) <= now && now <= new Date(event.endAt)
}

function eventTypeLabel(type) {
  return {
    MONTHLY_FLAVOR: '이달의 맛', SIZE_UP: '사이즈업', AMOUNT_DISCOUNT: '금액 할인',
    COUPON: '쿠폰', NEW_PRODUCT: '신제품', STOCK_CLEARANCE: '재고 정리'
  }[type] || type
}
function benefitTypeLabel(type) {
  return { NONE: '없음', DISCOUNT_RATE: '할인율', DISCOUNT_AMOUNT: '할인금액', SIZE_UP: '사이즈업', COUPON: '쿠폰' }[type] || type
}
function statusLabel(status) {
  return { DRAFT: '초안', SCHEDULED: '예정', ACTIVE: '진행중', ENDED: '종료', CANCELLED: '취소' }[status] || status
}
function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}header{display:flex;align-items:flex-end;justify-content:space-between}header p{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}h1{margin:0;font-size:27px}header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}.refresh{padding:10px 14px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:9px;font-weight:800;cursor:pointer}
.invite-card{margin-top:24px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}.invite-card h2{margin:0 0 12px;font-size:14px}
.event-form{display:grid;grid-template-columns:1.4fr 1fr 1fr;gap:8px}.event-form textarea{grid-column:1/-1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px;font-family:inherit;resize:vertical}.event-form input,.event-form select{padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}.event-form button{grid-column:1/-1;padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}.event-form button:disabled{opacity:.55}
.alert{margin-top:10px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.issued{margin-top:10px;padding:13px;color:#0b9654;background:#e2f8ec;border:1px solid #c7f0da;border-radius:9px;font-size:11px}
.section-desc{margin:0 0 12px;color:#8c95a2;font-size:11px}
.coupon-row{display:grid;grid-template-columns:1fr 200px 90px 90px 130px;gap:16px;align-items:center;padding:16px 22px;border-bottom:1px solid #eef1f5}.coupon-row:last-child{border:0}.coupon-row strong{font-size:12px}.coupon-row p{margin:4px 0 0;color:#88919e;font-size:10px}.coupon-row small{color:#98a1ae;font-size:9px;text-align:right}
.coupon-code{overflow:hidden;padding:6px 8px;color:#5d62e8;background:#f0f1ff;border-radius:6px;font-size:9px;text-overflow:ellipsis;white-space:nowrap}
.list-card{overflow:hidden;margin-top:20px;background:#fff;border:1px solid #e4e8ef;border-radius:16px}.list-head{display:flex;align-items:center;justify-content:space-between;padding:19px 22px;border-bottom:1px solid #e9edf2}.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.event-row{display:grid;grid-template-columns:1fr 110px 90px 220px;gap:16px;align-items:center;padding:16px 22px;border-bottom:1px solid #eef1f5}.event-row:last-child{border:0}.event-row strong{font-size:12px}.event-row p{margin:4px 0 0;color:#88919e;font-size:10px}.event-row small{color:#98a1ae;font-size:9px;text-align:right}
.tag{padding:5px 8px;color:#5d62e8;background:#f0f1ff;border-radius:6px;font-size:9px;font-weight:800;text-align:center}
.status{padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800;text-align:center}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.event-form{grid-template-columns:1fr}.event-row{grid-template-columns:1fr}.coupon-row{grid-template-columns:1fr}}
</style>
