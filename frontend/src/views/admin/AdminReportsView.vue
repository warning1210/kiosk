<template>
  <main class="page">
    <AdminSidebar active="reports" />

    <section class="content">
      <AdminPageHeader title="통계/리포트" subtitle="전체 매출, 주문, 고객 데이터를 기반으로 전점 현황을 분석할 수 있습니다." />

      <div v-if="error" class="alert">{{ error }}</div>

      <div v-if="loading" class="empty">불러오는 중입니다.</div>
      <template v-else>
        <div class="summary">
          <AdminStatCard icon="💰" label="이번달 총매출" :value="`₩${stats.monthSales.toLocaleString()}`" />
          <AdminStatCard icon="🧾" label="이번달 총 주문 건수" :value="`${stats.monthOrderCount.toLocaleString()}건`" tone="blue" />
          <AdminStatCard icon="📊" label="평균 객단가" :value="`₩${stats.avgOrderValue.toLocaleString()}`" tone="orange" />
          <AdminStatCard
            icon="📈"
            label="전월 대비 성장률"
            :value="growthLabel"
            :tone="growthTone"
          />
        </div>

        <section class="chart-card">
          <div class="list-head">
            <div>
              <h2>지점별 매출 비교</h2>
              <span>이번달 지점별 매출을 서로 비교해서 볼 수 있습니다.</span>
            </div>
          </div>

          <div v-if="!stats.branchRanking.length" class="empty">이번달 매출 데이터가 없습니다.</div>
          <div v-else class="bars">
            <div v-for="(row, index) in stats.branchRanking" :key="row.branchId">
              <b>₩{{ compactWon(row.sales) }}</b>
              <i :style="{ height: percent(row.sales, maxSales) + '%', background: barColors[index % barColors.length] }"></i>
              <span>{{ row.branchName }}</span>
            </div>
          </div>
        </section>

        <section class="list-card">
          <div class="list-head">
            <div>
              <h2>지점별 매출 순위</h2>
              <span>이번달 매출이 높은 순서대로 전 지점을 보여줍니다.</span>
            </div>
          </div>

          <div v-if="!stats.branchRanking.length" class="empty">이번달 매출 데이터가 없습니다.</div>
          <table v-else>
            <thead><tr><th>순위</th><th>지점명</th><th>매출</th><th>주문 건수</th></tr></thead>
            <tbody>
              <tr v-for="(row, index) in stats.branchRanking" :key="row.branchId">
                <td><span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span></td>
                <td><strong>{{ row.branchName }}</strong></td>
                <td>₩{{ row.sales.toLocaleString() }}</td>
                <td>{{ row.orderCount.toLocaleString() }}건</td>
              </tr>
            </tbody>
          </table>
        </section>

        <div class="grid2">
          <section class="chart-card">
            <div class="list-head">
              <div>
                <h2>상품별 통계</h2>
                <span>이번달 전 지점 통합 기준, 가장 많이 팔린 맛 TOP 5입니다.</span>
              </div>
            </div>
            <div v-if="!stats.flavorSalesRanking.length" class="empty">이번달 판매 데이터가 없습니다.</div>
            <div v-else class="line-chart">
              <svg viewBox="0 0 640 190" preserveAspectRatio="none">
                <g class="y-axis">
                  <line v-for="tick in yAxisTicks(stats.flavorSalesRanking)" :key="tick.y" x1="52" :y1="tick.y" x2="630" :y2="tick.y" class="grid-line" />
                  <text v-for="tick in yAxisTicks(stats.flavorSalesRanking)" :key="'l' + tick.y" x="46" :y="tick.y + 3" text-anchor="end" class="axis-label">{{ tick.value.toLocaleString() }}</text>
                </g>
                <path class="area" :d="areaPath(stats.flavorSalesRanking)" />
                <polyline :points="linePoints(stats.flavorSalesRanking)" />
                <circle v-for="(point, index) in pointList(stats.flavorSalesRanking)" :key="index" :cx="point.x" :cy="point.y" r="4" />
              </svg>
              <div class="labels"><span v-for="row in stats.flavorSalesRanking" :key="row.flavorId">{{ row.flavorName }}</span></div>
            </div>
          </section>

          <section class="chart-card">
            <div class="list-head">
              <div>
                <h2>상품 신청 건수</h2>
                <span>이번달 지점에서 어떤 맛을 가장 많이 재고 신청했는지 TOP 5입니다.</span>
              </div>
            </div>
            <div v-if="!stats.flavorRequestRanking.length" class="empty">이번달 재고 신청 데이터가 없습니다.</div>
            <div v-else class="line-chart">
              <svg viewBox="0 0 640 190" preserveAspectRatio="none">
                <g class="y-axis">
                  <line v-for="tick in yAxisTicks(stats.flavorRequestRanking)" :key="tick.y" x1="52" :y1="tick.y" x2="630" :y2="tick.y" class="grid-line" />
                  <text v-for="tick in yAxisTicks(stats.flavorRequestRanking)" :key="'l' + tick.y" x="46" :y="tick.y + 3" text-anchor="end" class="axis-label">{{ tick.value.toLocaleString() }}</text>
                </g>
                <path class="area" :d="areaPath(stats.flavorRequestRanking)" />
                <polyline :points="linePoints(stats.flavorRequestRanking)" />
                <circle v-for="(point, index) in pointList(stats.flavorRequestRanking)" :key="index" :cx="point.x" :cy="point.y" r="4" />
              </svg>
              <div class="labels"><span v-for="row in stats.flavorRequestRanking" :key="row.flavorId">{{ row.flavorName }}</span></div>
            </div>
          </section>
        </div>
      </template>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'

const barColors = ['#6266ef', '#8d9af3', '#ee69ae', '#ffb31a', '#2ec893', '#9b7be9', '#ff9417', '#bea7ef']

const loading = ref(true)
const error = ref('')
const stats = ref({
  monthSales: 0, monthOrderCount: 0, avgOrderValue: 0, growthRatePercent: null,
  branchRanking: [], flavorSalesRanking: [], flavorRequestRanking: []
})

const maxSales = computed(() => Math.max(1, ...stats.value.branchRanking.map(row => Number(row.sales))))

function percent(value, max) {
  return Math.max(4, Number(value) / Number(max) * 100)
}
function compactWon(value) {
  if (value >= 100000000) return `${(value / 100000000).toFixed(1)}억`
  if (value >= 10000) return `${Math.round(value / 10000)}만`
  return value.toLocaleString()
}

const CHART_X_START = 55
const CHART_X_END = 630

function pointList(rows) {
  const max = Math.max(1, ...rows.map(r => Number(r.value)))
  const width = CHART_X_END - CHART_X_START
  return rows.map((r, i) => ({
    x: rows.length === 1 ? CHART_X_START + width / 2 : CHART_X_START + i * width / (rows.length - 1),
    y: 170 - Number(r.value) / max * 135,
    value: Number(r.value)
  }))
}
function linePoints(rows) {
  return pointList(rows).map(p => `${p.x},${p.y}`).join(' ')
}
function areaPath(rows) {
  const points = pointList(rows)
  return points.length ? `M ${CHART_X_START} 180 L ${points.map(p => `${p.x} ${p.y}`).join(' L ')} L ${CHART_X_END} 180 Z` : ''
}
function yAxisTicks(rows) {
  const max = Math.max(1, ...rows.map(r => Number(r.value)))
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, i) => {
    const ratio = i / steps
    return { value: Math.round(max * ratio), y: 170 - ratio * 135 }
  })
}

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = (await http.get('/hq/dashboard/stats')).data
  } catch (e) {
    error.value = e.response?.data?.message || '통계를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const growthLabel = computed(() => {
  const rate = stats.value.growthRatePercent
  if (rate === null || rate === undefined) return '집계 불가'
  return `${rate > 0 ? '+' : ''}${rate}%`
})
const growthTone = computed(() => {
  const rate = stats.value.growthRatePercent
  if (rate === null || rate === undefined) return 'default'
  return rate >= 0 ? 'green' : 'pink'
})
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.alert{margin-top:14px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:24px 0}
.chart-card{overflow:hidden;margin-bottom:20px;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.bars{display:flex;height:220px;gap:14px;align-items:end;overflow-x:auto;padding:8px 22px 22px}
.bars>div{display:flex;flex:1 0 60px;height:100%;flex-direction:column;justify-content:flex-end;align-items:center}
.bars i{display:block;width:100%;min-height:5px;border-radius:7px 7px 2px 2px}
.bars b{margin-bottom:6px;font-size:10px;font-weight:800;white-space:nowrap}
.bars span{overflow:hidden;width:100%;margin-top:8px;text-align:center;text-overflow:ellipsis;white-space:nowrap;font-size:9px;color:#697487}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
table{width:100%;border-collapse:collapse;font-size:11px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:14px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
.rank{display:inline-grid;width:22px;height:22px;place-items:center;color:#697487;background:#eef0f3;border-radius:50%;font-weight:800}
.rank.top{color:#fff;background:#5f63ee}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:20px}
.grid2 .chart-card{margin-bottom:0;padding-bottom:18px}
.line-chart{padding:14px 22px 0}
.line-chart svg{width:100%;height:180px;border-bottom:1px solid #e8ecf2}
.line-chart polyline{fill:none;stroke:#6266ef;stroke-width:3}
.line-chart circle{fill:#fff;stroke:#6266ef;stroke-width:2}
.line-chart .area{fill:#f0f1ff;stroke:none}
.line-chart .grid-line{stroke:#eef0f3;stroke-width:1}
.line-chart .axis-label{fill:#98a1ae;font-size:9px}
.labels{display:flex;justify-content:space-between;gap:4px;margin-top:8px;padding:0 1.6% 0 8.6%;color:#8993a2;font-size:8px}
.labels span{overflow:hidden;max-width:70px;text-overflow:ellipsis;white-space:nowrap}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}table{display:block;overflow-x:auto}.grid2{grid-template-columns:1fr}}
</style>
