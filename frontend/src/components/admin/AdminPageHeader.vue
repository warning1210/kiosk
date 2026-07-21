<template>
  <header class="admin-page-header">
    <div>
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <span>{{ subtitle }}</span>
    </div>
    <div class="tools">
      <label class="range"><i>📅</i>{{ dateRange }}</label>
      <button class="bell" type="button">🔔</button>
      <div class="profile"><span>{{ nameInitial }}</span>{{ session.name }}</div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

defineProps({
  eyebrow: { type: String, default: '본점 관리' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  dateRange: { type: String, default: '2024.04.01 ~ 2024.04.30' }
})

const session = JSON.parse(localStorage.getItem('hq-session') || '{}')
if (!session.name) session.name = '본점 관리자'
const nameInitial = computed(() => session.name.slice(0, 1))
</script>

<style scoped>
.admin-page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;flex-wrap:wrap}
.admin-page-header .eyebrow{margin:0 0 7px;color:#666bef;font-size:10px;font-weight:900}
.admin-page-header h1{margin:0;font-size:27px}
.admin-page-header span{display:block;margin-top:7px;color:#7d8796;font-size:11px}
.tools{display:flex;align-items:center;gap:10px}
.range{display:flex;align-items:center;gap:8px;padding:10px 14px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:11px;font-weight:700}
.range i{font-style:normal}
.bell{display:grid;width:38px;height:38px;place-items:center;border:1px solid #dfe3e9;background:#fff;border-radius:9px;font-size:14px;cursor:pointer}
.profile{display:flex;align-items:center;gap:8px;padding:7px 14px 7px 7px;color:#4e5868;border:1px solid #dfe3e9;background:#fff;border-radius:20px;font-size:11px;font-weight:800}
.profile span{display:grid;width:26px;height:26px;place-items:center;color:#fff;background:#6266f2;border-radius:50%;font-weight:800}
@media(max-width:760px){.admin-page-header{flex-direction:column;align-items:flex-start}.tools{width:100%}.range{flex:1}}
</style>
