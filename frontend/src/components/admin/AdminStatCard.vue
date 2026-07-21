<template>
  <article class="admin-stat-card" :class="tone">
    <div class="top">
      <span class="icon">{{ icon }}</span>
      <span class="label">{{ label }}</span>
    </div>
    <b>{{ value }}</b>
    <small v-if="delta">{{ delta }}</small>
  </article>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '▦' },
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  delta: { type: String, default: '' },
  tone: { type: String, default: 'default' }
})
</script>

<style scoped>
.admin-stat-card{display:grid;gap:12px;padding:18px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}
.top{display:flex;align-items:center;justify-content:space-between}
.label{color:#798392;font-size:11px;font-weight:700}
.icon{display:grid;width:30px;height:30px;place-items:center;background:#eef0ff;border-radius:9px;font-size:13px}
b{font-size:22px}
small{color:#8c95a2;font-size:10px}
.admin-stat-card.pink .icon{background:#ffe8f2}
.admin-stat-card.orange .icon{background:#fff3d6}
.admin-stat-card.blue .icon{background:#e4f0ff}
.admin-stat-card.green .icon{background:#e2f8ec}
</style>
