<template>
  <div class="shell">
    <BranchSidebar active="sales" />
    <main>
      <header>
        <div>
          <h1>판매 통계</h1>
          <p>키오스크 결제 완료 주문 기준 · 상품/사이즈/시간대/월별</p>
        </div>
        <button @click="load">↻ 새로고침</button>
      </header>

      <section class="summary">
        <article>
          <span>누적 매출</span>
          <b>₩{{ number(summary.revenue) }}</b>
          <small>결제 승인 금액 합계</small>
        </article>
        <article>
          <span>총 주문</span>
          <b>{{ number(summary.order_count) }}건</b>
          <small>취소·결제대기 제외</small>
        </article>
        <article>
          <span>객단가</span>
          <b>₩{{ number(summary.average_amount) }}</b>
          <small>주문당 평균 결제액</small>
        </article>
      </section>

      <section class="grid">
        <article>
          <div class="card-title">
            <h2>상품별 판매</h2>
            <span>맛 기준 TOP 10</span>
          </div>
          <div v-for="(row, index) in products" :key="row.label" class="rank-row">
            <b>{{ row.label }}</b>
            <div>
              <i :style="{ width: percent(row.quantity, maxProduct) + '%', background: colors[index % colors.length] }"></i>
            </div>
            <strong>{{ row.quantity }}개</strong>
          </div>
        </article>

        <article>
          <div class="card-title">
            <h2>사이즈별 판매</h2>
            <span>판매 수량</span>
          </div>
          <div class="bars">
            <div v-for="(row, index) in sizes" :key="row.label">
              <b>{{ row.quantity }}</b>
              <i :style="{ height: percent(row.quantity, maxSize) + '%', background: barColors[index % barColors.length] }"></i>
              <span>{{ row.label }}</span>
            </div>
          </div>
        </article>

        <article>
          <div class="card-title">
            <h2>시간대별 주문량</h2>
            <span>결제 시간 기준</span>
          </div>
          <div class="line-chart">
            <svg viewBox="0 0 600 190" preserveAspectRatio="none">
              <path class="area" :d="areaPath(hourly)" />
              <polyline :points="linePoints(hourly)" />
              <circle v-for="(point, index) in pointList(hourly)" :key="index" :cx="point.x" :cy="point.y" r="4" />
            </svg>
            <div class="labels">
              <span v-for="row in hourly" :key="row.label">{{ row.label }}</span>
            </div>
          </div>
        </article>

        <article>
          <div class="card-title">
            <h2>월별 주문량</h2>
            <span>월 매출 포함</span>
          </div>
          <div class="monthly">
            <div v-for="(row, index) in monthly" :key="row.label">
              <span>{{ row.label }}</span>
              <div>
                <i :style="{ width: percent(row.quantity, maxMonth) + '%', background: index === monthly.length - 1 ? '#6266ef' : '#cbd3ff' }"></i>
              </div>
              <b>{{ row.quantity }}건</b>
              <small>₩{{ number(row.revenue) }}</small>
            </div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const summary = ref({})
const products = ref([])
const sizes = ref([])
const hourly = ref([])
const monthly = ref([])

const colors = ['#ee69ae', '#9b84ef', '#ffb31a', '#2ec893', '#ff9417']
const barColors = ['#8d9af3', '#6266ef', '#9b7be9', '#bea7ef', '#eb6aa9']

const maxProduct = computed(() => Math.max(1, ...products.value.map(r => Number(r.quantity))))
const maxSize = computed(() => Math.max(1, ...sizes.value.map(r => Number(r.quantity))))
const maxMonth = computed(() => Math.max(1, ...monthly.value.map(r => Number(r.quantity))))

const number = v => Number(v || 0).toLocaleString('ko-KR')
const percent = (v, max) => Math.max(4, Number(v) / Number(max) * 100)

async function load() {
  const { data } = await http.get('/sales')
  summary.value = data.summary
  products.value = data.flavors
  sizes.value = data.sizes
  hourly.value = data.hourly
  monthly.value = data.monthly
}

function pointList(rows) {
  const max = Math.max(1, ...rows.map(r => Number(r.quantity)))
  return rows.map((r, i) => ({
    x: rows.length === 1 ? 300 : i * 600 / (rows.length - 1),
    y: 170 - Number(r.quantity) / max * 135
  }))
}

function linePoints(rows) {
  return pointList(rows).map(p => `${p.x},${p.y}`).join(' ')
}

function areaPath(rows) {
  const p = pointList(rows)
  return p.length ? `M 0 180 L ${p.map(x => `${x.x} ${x.y}`).join(' L ')} L 600 180 Z` : ''
}

onMounted(load)
</script>

<style scoped>
.shell { min-height: 100vh; color: #202a39; background: #f3f6fa }
main { margin-left: 238px; padding: 32px 34px 55px }
header { display: flex; align-items: center; justify-content: space-between }
h1 { margin: 0; font-size: 27px }
header p { margin: 7px 0; color: #7e8997; font-size: 11px }
header button { padding: 10px 14px; color: #5f64e9; border: 1px solid #d9def2; background: #fff; border-radius: 9px; font-size: 10px; font-weight: 800 }
.summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 24px }
.summary article, .grid article { padding: 20px; border: 1px solid #e2e7ee; background: #fff; border-radius: 16px }
.summary span, .summary b, .summary small { display: block }
.summary span { color: #778290; font-size: 11px }
.summary b { margin: 14px 0 7px; font-size: 24px }
.summary small { color: #18a45c; font-size: 9px }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px }
.grid article { min-height: 285px }
.card-title { display: flex; align-items: center; justify-content: space-between }
.card-title h2 { margin: 0; font-size: 14px }
.card-title span { color: #8d96a4; font-size: 9px }
.rank-row { display: grid; grid-template-columns: 95px 1fr 38px; gap: 9px; align-items: center; margin-top: 16px }
.rank-row > b, .rank-row > strong { font-size: 9px }
.rank-row > strong { text-align: right }
.rank-row > div { height: 8px; background: #edf1f6; border-radius: 9px }
.rank-row i { display: block; height: 100%; border-radius: 9px }
.bars { display: flex; height: 220px; gap: 12px; align-items: end; padding: 20px 4px 0 }
.bars > div { display: flex; flex: 1; height: 100%; flex-direction: column; justify-content: flex-end; align-items: center }
.bars i { display: block; width: 100%; min-height: 5px; border-radius: 7px 7px 2px 2px }
.bars b { margin-bottom: 5px; font-size: 9px }
.bars span { overflow: hidden; width: 100%; margin-top: 8px; text-align: center; text-overflow: ellipsis; white-space: nowrap; font-size: 8px }
.line-chart { height: 220px; margin-top: 18px }
.line-chart svg { width: 100%; height: 180px; border-bottom: 1px solid #e8ecf2 }
.line-chart polyline { fill: none; stroke: #6266ef; stroke-width: 3 }
.line-chart circle { fill: #fff; stroke: #6266ef; stroke-width: 2 }
.line-chart .area { fill: #f0f1ff; stroke: none }
.labels { display: flex; justify-content: space-between; color: #8993a2; font-size: 8px }
.monthly { margin-top: 13px }
.monthly > div { display: grid; grid-template-columns: 52px 1fr 38px 75px; gap: 8px; align-items: center; margin-top: 12px; font-size: 9px }
.monthly > div > div { height: 9px; background: #eff2f7; border-radius: 8px }
.monthly i { display: block; height: 100%; border-radius: 8px }
.monthly small { text-align: right; color: #7d8795 }

@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr }
}
@media (max-width: 760px) {
  main { margin-left: 0; padding: 20px }
  .summary { grid-template-columns: 1fr }
  .grid { grid-template-columns: 1fr }
}
</style>
