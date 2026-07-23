<template>
  <div class="shell"><BranchSidebar active="events" /><main>
    <header><div><h1>이벤트 관리</h1><p>진행 중인 상품(맛) 할인 이벤트입니다. 지점이 맛을 직접 골라야 하는 이벤트는 아래에서 적용하세요.</p></div></header>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="!loading && !events.length" class="empty">진행 중인 상품(맛) 할인 이벤트가 없습니다.</div>

    <section v-for="event in events" :key="event.eventId" class="event-card">
      <div class="event-head">
        <div>
          <strong>{{ event.eventName }}</strong>
          <span class="period">{{ formatDate(event.startAt) }} ~ {{ formatDate(event.endAt) }}</span>
        </div>
        <span class="discount">{{ discountLabel(event) }}</span>
      </div>

      <template v-if="event.eventType === 'HQ_FLAVOR_DISCOUNT'">
        <p class="current">본점이 지정한 맛: <strong>{{ event.selectedFlavorName }}</strong> (본점 지정 · 지점에서 변경 불가)</p>
      </template>
      <template v-else>
        <div class="flavor-row">
          <select v-model="selections[event.eventId]" :disabled="!event.flavorOptions.length">
            <option value="" disabled>{{ event.flavorOptions.length ? '맛을 선택하세요' : '우리 지점에 등록된 맛이 없습니다' }}</option>
            <option v-for="flavor in event.flavorOptions" :key="flavor.flavorId" :value="flavor.flavorId">{{ flavor.flavorName }}</option>
          </select>
          <button
            type="button"
            :disabled="saving === event.eventId || !selections[event.eventId]"
            @click="saveSelection(event)"
          >{{ saving === event.eventId ? '저장 중...' : '적용' }}</button>
        </div>
        <p v-if="event.selectedFlavorName" class="current">현재 적용 중: <strong>{{ event.selectedFlavorName }}</strong></p>
      </template>
    </section>
  </main></div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const events = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(null)
const selections = reactive({})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    events.value = (await http.get('/events')).data
    for (const event of events.value) {
      selections[event.eventId] = event.selectedFlavorId || ''
    }
  } catch (e) {
    error.value = e.response?.data?.message || '이벤트 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function saveSelection(event) {
  saving.value = event.eventId
  error.value = ''
  try {
    const { data } = await http.post(`/events/${event.eventId}/flavor`, { flavorId: selections[event.eventId] })
    Object.assign(event, data)
  } catch (e) {
    error.value = e.response?.data?.message || '맛을 적용하지 못했습니다.'
  } finally {
    saving.value = null
  }
}

function discountLabel(event) {
  return event.benefitType === 'DISCOUNT_RATE' ? `${event.discountRate}% 할인` : `₩${(event.discountAmount ?? 0).toLocaleString()} 할인`
}
function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value))
}
</script>

<style scoped>
.shell{min-height:100vh;color:#1f2938;background:#f3f6fa}main{margin-left:238px;padding:32px 34px 55px}header h1{margin:0;font-size:27px}header p{margin:7px 0 0;color:#7f8997;font-size:11px}
.alert{margin-top:20px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.empty{margin-top:24px;padding:50px;color:#929ba7;text-align:center;font-size:11px;background:#fff;border:1px solid #e5e9ef;border-radius:16px}
.event-card{margin-top:16px;padding:20px 22px;background:#fff;border:1px solid #e5e9ef;border-radius:16px}
.event-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap}
.event-head strong{display:block;font-size:14px}
.period{display:block;margin-top:4px;color:#8c95a2;font-size:10px}
.discount{padding:6px 10px;color:#5d62e8;background:#f0f1ff;border-radius:8px;font-size:12px;font-weight:800}
.flavor-row{display:flex;gap:8px;margin-top:16px}
.flavor-row select{flex:1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}
.flavor-row button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer;white-space:nowrap}
.flavor-row button:disabled{opacity:.55;cursor:not-allowed}
.current{margin:12px 0 0;color:#0b9654;font-size:11px}
@media(max-width:760px){main{margin-left:0;padding:20px}}
</style>
