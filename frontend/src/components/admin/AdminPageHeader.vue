<template>
  <header class="admin-page-header">
    <div>
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <span>{{ subtitle }}</span>
    </div>
    <div class="tools">
      <label class="range"><i><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg></i>{{ displayedDate }}</label>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  eyebrow: { type: String, default: '본점 관리' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  // 특정 화면이 직접 날짜를 넘기지 않으면 현재 한국 날짜와 시간을 표시한다.
  dateRange: { type: String, default: '' }
})

const session = JSON.parse(localStorage.getItem('hq-session') || '{}')
if (!session.name) session.name = '본점 관리자'
const nameInitial = computed(() => session.name.slice(0, 1))
const currentTime = ref(new Date())
let clockTimer

// 브라우저 PC의 시간대와 관계없이 본점 화면은 한국 표준시로 표시한다.
const koreanTime = computed(() => new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  weekday: 'short',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
}).format(currentTime.value))
const displayedDate = computed(() => props.dateRange || koreanTime.value)

onMounted(() => {
  // 실제 시계처럼 보이도록 1초마다 현재 시각을 다시 계산한다.
  clockTimer = window.setInterval(() => { currentTime.value = new Date() }, 1000)
})
onBeforeUnmount(() => window.clearInterval(clockTimer))
</script>

<style scoped>
.admin-page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;flex-wrap:wrap}
.admin-page-header .eyebrow{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}
.admin-page-header h1{margin:0;font-size:27px}
.admin-page-header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}
.tools{display:flex;align-items:center;gap:10px}
.range{display:flex;align-items:center;gap:8px;padding:10px 14px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:11px;font-weight:700}
.range i{display:grid;place-items:center;font-style:normal}.range i svg{width:14px;height:14px;display:block}
.bell{display:grid;width:38px;height:38px;place-items:center;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:14px;cursor:pointer}
.profile{display:flex;align-items:center;gap:8px;padding:7px 14px 7px 7px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:20px;font-size:11px;font-weight:800}
.profile span{display:grid;width:26px;height:26px;place-items:center;color:#fff;background:#6266f2;border-radius:50%;font-weight:800}
@media(max-width:760px){.admin-page-header{flex-direction:column;align-items:flex-start}.tools{width:100%}.range{flex:1}}
</style>
