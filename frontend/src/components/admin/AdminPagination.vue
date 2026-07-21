<template>
  <nav v-if="pageCount > 1" class="admin-pagination">
    <button type="button" :disabled="modelValue <= 1" @click="go(modelValue - 1)">‹</button>
    <button
      v-for="page in pages"
      :key="page"
      type="button"
      :class="{ active: page === modelValue }"
      @click="go(page)"
    >{{ page }}</button>
    <span v-if="showTrailingEllipsis">…</span>
    <button type="button" :disabled="modelValue >= pageCount" @click="go(modelValue + 1)">›</button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, required: true },
  total: { type: Number, required: true },
  pageSize: { type: Number, default: 10 },
  maxButtons: { type: Number, default: 5 }
})
const emit = defineEmits(['update:modelValue'])

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const pages = computed(() => {
  const count = Math.min(props.maxButtons, pageCount.value)
  const start = Math.min(Math.max(1, props.modelValue - Math.floor(count / 2)), pageCount.value - count + 1)
  return Array.from({ length: count }, (_, i) => start + i)
})
const showTrailingEllipsis = computed(() => pages.value[pages.value.length - 1] < pageCount.value)

function go(page) {
  if (page < 1 || page > pageCount.value) return
  emit('update:modelValue', page)
}
</script>

<style scoped>
.admin-pagination{display:flex;align-items:center;justify-content:center;gap:5px;padding:16px}
.admin-pagination button{display:grid;min-width:30px;height:30px;place-items:center;padding:0 6px;color:#697487;border:1px solid #e2e5ea;background:#fff;border-radius:8px;font-size:11px;font-weight:700;cursor:pointer}
.admin-pagination button.active{color:#fff;border-color:#5f63ee;background:#5f63ee}
.admin-pagination button:disabled{opacity:.4;cursor:default}
.admin-pagination span{color:#9299a5;font-size:11px}
</style>
