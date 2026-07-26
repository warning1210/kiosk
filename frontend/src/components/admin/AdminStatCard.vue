<template>
  <article class="admin-stat-card" :class="tone">
    <div class="top">
      <span class="icon" v-html="iconSvg"></span>
      <span class="label">{{ label }}</span>
    </div>
    <b>{{ value }}</b>
    <small v-if="delta">{{ delta }}</small>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: '▦' },
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  delta: { type: String, default: '' },
  tone: { type: String, default: 'default' }
})

// 기존 호출부가 넘기던 이모지/기호를 통일된 선형 SVG 아이콘으로 매핑한다.
// (각 화면의 <AdminStatCard icon="📋" ...> 는 고칠 필요 없이 그대로 두면 됨)
const ICONS = {
  '📋': '<rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>',
  '▦': '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>',
  '⏱': '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  '🕒': '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  '🚚': '<path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h1"/><path d="M15 18H9"/><path d="M19 18h1a1 1 0 0 0 1-1v-3.3a1 1 0 0 0-.2-.6l-2.5-3.1a1 1 0 0 0-.8-.4H14"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/>',
  '✉': '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m2 7 10 6 10-6"/>',
  '⌂': '<path d="M3 9 12 3l9 6"/><path d="M5 10v10h14V10"/><path d="M9 20v-6h6v6"/>',
  '✓': '<circle cx="12" cy="12" r="9"/><path d="m8.5 12 2.5 2.5 4.5-5"/>',
  '!': '<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  '⚠': '<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  '⏸': '<circle cx="12" cy="12" r="9"/><path d="M10 9v6M14 9v6"/>',
  '▨': '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v5h5"/><path d="M8 13h8M8 17h6"/>',
  '✎': '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  '✕': '<circle cx="12" cy="12" r="9"/><path d="m15 9-6 6M9 9l6 6"/>',
  '🎁': '<path d="M20 12v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-8"/><rect x="2" y="7" width="20" height="5" rx="1"/><path d="M12 22V7"/><path d="M12 7S9.5 7 8.5 5.5 9 2.5 10 3s2 4 2 4"/><path d="M12 7s2.5 0 3.5-1.5S15 2.5 14 3s-2 4-2 4"/>',
  '🎫': '<path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/><path d="M13 5v2M13 11v2M13 17v2"/>',
  '💰': '<rect x="2" y="6" width="20" height="13" rx="2"/><path d="M2 10h20"/><circle cx="17" cy="14" r="1.3"/>',
  '📈': '<path d="M22 7 13.5 15.5l-5-5L2 17"/><path d="M16 7h6v6"/>',
  '📊': '<path d="M3 3v18h18"/><path d="M7 15v3M12 9v9M17 5v13"/>',
  '📦': '<path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="M3.3 7 12 12l8.7-5"/><path d="M12 22V12"/>',
  '🧾': '<path d="M4 2h16v20l-3-2-3 2-2-2-2 2-3-2-3 2V2Z"/><path d="M8 7h8M8 11h8M8 15h5"/>'
}

const iconSvg = computed(() => {
  const inner = ICONS[props.icon] || ICONS['▦']
  return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${inner}</svg>`
})
</script>

<style scoped>
.admin-stat-card{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}
.top{display:flex;align-items:center;justify-content:space-between}
.label{color:#798392;font-size:11px;font-weight:700}
.icon{display:grid;width:32px;height:32px;place-items:center;background:#eef0ff;border-radius:9px;color:#5b61e6}
.icon :deep(svg){width:17px;height:17px;display:block}
b{font-size:22px}
small{color:#8c95a2;font-size:10px}
.admin-stat-card.pink .icon{background:#ffe8f2;color:#e83e8c}
.admin-stat-card.orange .icon{background:#fff3d6;color:#e0900c}
.admin-stat-card.blue .icon{background:#e4f0ff;color:#2f7ad6}
.admin-stat-card.green .icon{background:#e2f8ec;color:#1faa5f}
</style>
