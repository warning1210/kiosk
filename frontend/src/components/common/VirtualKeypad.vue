<template>
  <div class="vkeypad" @mousedown.prevent>
    <div class="vkeypad-header">
      <span>가상 키패드</span>
      <button type="button" class="vkeypad-close" @click="$emit('close')" aria-label="키패드 닫기">✕</button>
    </div>

    <div class="vkeypad-row">
      <button v-for="n in numberKeys" :key="n" type="button" class="key" @click="press(n)">{{ n }}</button>
      <button type="button" class="key wide" @click="backspace">⌫</button>
    </div>

    <div class="vkeypad-row">
      <button v-for="k in row1" :key="k" type="button" class="key" @click="press(k)">{{ display(k) }}</button>
    </div>

    <div class="vkeypad-row indent">
      <button v-for="k in row2" :key="k" type="button" class="key" @click="press(k)">{{ display(k) }}</button>
    </div>

    <div class="vkeypad-row">
      <button type="button" class="key wide" :class="{ active: shift }" @click="toggleShift">⇧ Shift</button>
      <button v-for="k in row3" :key="k" type="button" class="key" @click="press(k)">{{ display(k) }}</button>
    </div>

    <div class="vkeypad-row">
      <button type="button" class="key space" @click="space">Space</button>
      <button type="button" class="key wide" @click="clearAll">지우기</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'close'])

const numberKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0']
const row1 = ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p']
const row2 = ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l']
const row3 = ['z', 'x', 'c', 'v', 'b', 'n', 'm']

const shift = ref(false)

function display(key) {
  return shift.value ? key.toUpperCase() : key
}

function press(key) {
  const char = /[a-z]/i.test(key) && shift.value ? key.toUpperCase() : key
  emit('update:modelValue', props.modelValue + char)
}

function backspace() {
  emit('update:modelValue', props.modelValue.slice(0, -1))
}

function space() {
  emit('update:modelValue', props.modelValue + ' ')
}

function clearAll() {
  emit('update:modelValue', '')
}

function toggleShift() {
  shift.value = !shift.value
}
</script>

<style scoped>
.vkeypad{margin-top:8px;padding:10px;background:#f7f8fa;border:1px solid #dfe3e9;border-radius:10px;box-shadow:0 6px 16px rgb(38 51 71/8%)}
.vkeypad-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;font-size:10px;font-weight:800;color:#788391}
.vkeypad-close{width:22px;height:22px;padding:0;color:#788391;border:1px solid #dfe3e9;background:#fff;border-radius:6px;cursor:pointer;line-height:1}
.vkeypad-close:hover{color:#c52f47;border-color:#c52f47}
.vkeypad-row{display:flex;gap:5px;margin-top:5px}
.vkeypad-row:first-child{margin-top:0}
.vkeypad-row.indent{padding-left:14px}
.key{flex:1;padding:10px 0;color:#222b38;border:1px solid #dfe3e9;background:#fff;border-radius:6px;font-size:12px;font-weight:700;cursor:pointer}
.key:hover{background:#eef0ff;border-color:#6266ef}
.key.active{color:#fff;background:#6266ef;border-color:#6266ef}
.key.wide{flex:1.8}
.key.space{flex:5}
</style>
