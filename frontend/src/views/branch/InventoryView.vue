<template>
  <div class="admin-shell">
    <BranchSidebar active="inventory" />

    <main class="content">
      <header class="topbar">
        <div>
          <p class="eyebrow">BRANCH MANAGEMENT</p>
          <h1>재고 관리</h1>
          <p>맛별 재고를 확인하고 입고·차감·자동 발주를 처리합니다.</p>
        </div>
        <div class="top-actions">
          <span class="updated">마지막 저장 {{ lastSaved }}</span>
          <button class="icon-button" type="button">🔔<b v-if="lowCount">{{ lowCount }}</b></button>
          <div class="auto-order"><i></i><span>자동 발주</span><strong>ON</strong></div>
        </div>
      </header>

      <section class="summary-grid">
        <article>
          <div class="summary-icon pink">▣</div>
          <div><span>전체 맛</span><strong>{{ inventories.length }}개</strong><small>판매 등록 플레이버</small></div>
        </article>
        <article>
          <div class="summary-icon blue">◒</div>
          <div><span>총 보유 재고</span><strong>{{ totalTubs }}통</strong><small>{{ formatGram(totalGram) }}</small></div>
        </article>
        <article>
          <div class="summary-icon orange">!</div>
          <div><span>재고 부족</span><strong>{{ lowCount }}개</strong><small>1통(3kg) 이하</small></div>
        </article>
        <article>
          <div class="summary-icon green">⇢</div>
          <div><span>입고 예정</span><strong>{{ shippingCount }}건</strong><small>평균 배송 3일</small></div>
        </article>
      </section>

      <section class="inventory-panel">
        <div class="panel-head">
          <div class="tabs">
            <button v-for="tab in tabs" :key="tab.value" :class="{ active: filter === tab.value }" type="button" @click="filter = tab.value">
              {{ tab.label }} <span>{{ tabCount(tab.value) }}</span>
            </button>
          </div>
          <div class="tools">
            <label class="search"><span>⌕</span><input v-model="keyword" placeholder="맛 이름 검색"></label>
            <select v-model="sortBy"><option value="low">재고 적은 순</option><option value="name">이름순</option><option value="recent">최근 변경순</option></select>
          </div>
        </div>

        <div v-if="loading" class="empty">재고 데이터를 불러오는 중입니다.</div>
        <div v-else-if="filteredInventories.length === 0" class="empty">조건에 맞는 재고가 없습니다.</div>
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>아이스크림 맛</th><th>현재 재고</th><th>잔여량</th><th>상태</th><th>자동 신청 / 배송</th><th>재고 처리</th></tr></thead>
            <tbody>
              <tr v-for="item in filteredInventories" :key="item.id">
                <td>
                  <div class="flavor">
                    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name">
                    <div v-else class="flavor-placeholder">BR</div>
                    <div><strong>{{ item.name }}</strong><span>#{{ String(item.id).padStart(3, '0') }}</span></div>
                  </div>
                </td>
                <td><strong class="stock-number">{{ formatGram(item.remainingG) }}</strong><span class="tub-count">{{ (item.remainingG / 3000).toFixed(1) }}통</span></td>
                <td>
                  <div class="progress"><span :class="statusOf(item).key" :style="{ width: `${Math.min(item.remainingG / 6000 * 100, 100)}%` }"></span></div>
                  <small>{{ Math.round(item.remainingG / 6000 * 100) }}% / 기준 6kg</small>
                </td>
                <td><span class="status" :class="statusOf(item).key"><i></i>{{ statusOf(item).label }}</span></td>
                <td>
                  <div v-if="item.requestStatus === 'SHIPPING'" class="delivery"><strong>배송 중</strong><span>본점 배송 처리 중</span></div>
                  <div v-else-if="item.requestStatus === 'DELIVERED'" class="delivery done"><strong>배송 완료</strong><span>수량 확인 후 완료 확인 필요</span></div>
                  <div v-else-if="item.requestStatus === 'PENDING'" class="delivery pending"><strong>신청 대기</strong><span>본점 확인 중 · {{ item.requestTubs }}통</span></div>
                  <div v-else-if="item.requestStatus === 'COMPLETED'" class="delivery done"><strong>입고 완료</strong><span>재고 반영 완료</span></div>
                  <span v-else class="muted">재고 3kg 이하 시 자동 신청</span>
                </td>
                <td>
                  <div class="row-actions">
                    <button class="adjust" type="button" @click="openAdjust(item)">재고 조정</button>
                    <button v-if="!activeRequest(item)" class="request" type="button" @click="openRequest(item)">재고 신청</button>
                    <button v-if="item.requestStatus === 'DELIVERED'" class="receive" type="button" @click="completeDelivery(item)">배송 완료 확인</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <div v-if="modalItem" class="modal-backdrop" @click.self="closeModal">
      <section class="modal">
        <button class="close" type="button" @click="closeModal">×</button>
        <p class="eyebrow">INVENTORY ADJUSTMENT</p>
        <h2>{{ modalItem.name }} 재고 조정</h2>
        <p class="current">현재 재고 <strong>{{ formatGram(modalItem.remainingG) }}</strong> · {{ (modalItem.remainingG / 3000).toFixed(1) }}통</p>
        <div class="type-buttons">
          <button v-for="type in adjustTypes" :key="type.value" :class="{ active: adjustType === type.value }" type="button" @click="adjustType = type.value">{{ type.label }}</button>
        </div>
        <label>처리 중량(g)<input v-model.number="adjustGram" min="1" step="1" type="number"></label>
        <div class="quick-grams">
          <button v-for="size in quickSizes" :key="size.g" type="button" @click="adjustGram = size.g">{{ size.name }} {{ size.g }}g</button>
        </div>
        <label>처리 메모<input v-model="adjustMemo" type="text" placeholder="예: 폐기, 실사 조정, 수동 입고"></label>
        <div class="preview">처리 후 예상 재고 <strong>{{ formatGram(previewGram) }}</strong><span v-if="previewGram <= 3000 && !activeRequest(modalItem)">자동 발주 대상</span></div>
        <div class="modal-actions"><button type="button" @click="closeModal">취소</button><button class="primary" type="button" @click="applyAdjustment">재고 반영</button></div>
      </section>
    </div>

    <!-- 재고 현황에서 바로 본점에 재고를 신청하는 창 (BR-001) -->
    <div v-if="requestItem" class="modal-backdrop" @click.self="closeRequest">
      <section class="modal">
        <button class="close" type="button" @click="closeRequest">×</button>
        <p class="eyebrow">STOCK REQUEST</p>
        <h2>{{ requestItem.name }} 재고 신청</h2>
        <p class="current">
          현재 재고 <strong>{{ formatGram(requestItem.remainingG) }}</strong> · {{ (requestItem.remainingG / 3000).toFixed(1) }}통
        </p>

        <label>
          신청 수량
          <div class="qty">
            <button type="button" @click="requestTubs = Math.max(1, requestTubs - 1)">−</button>
            <input v-model.number="requestTubs" min="1" type="number">
            <span class="unit">통 ({{ (requestTubs * 3000).toLocaleString('ko-KR') }}g)</span>
            <button type="button" @click="requestTubs++">+</button>
          </div>
        </label>

        <label>
          긴급도
          <div class="type-buttons">
            <button v-for="option in urgencyOptions" :key="option.value"
                    :class="{ active: requestUrgency === option.value }" type="button"
                    @click="requestUrgency = option.value">
              {{ option.label }}
            </button>
          </div>
        </label>

        <label>신청 사유<input v-model="requestReason" type="text" placeholder="예: 주말 행사 대비 물량 확보"></label>

        <div class="preview">
          신청 후 예상 재고 <strong>{{ formatGram(requestItem.remainingG + requestTubs * 3000) }}</strong>
          <span>본점 승인 후 배송</span>
        </div>

        <p v-if="requestError" class="form-error">{{ requestError }}</p>

        <div class="modal-actions">
          <button type="button" @click="closeRequest">취소</button>
          <button class="primary" type="button" :disabled="requesting" @click="submitRequest">
            {{ requesting ? '신청 중...' : '본점에 신청' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="toast" class="toast">✓ {{ toast }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const branchSession = JSON.parse(localStorage.getItem('branch-session') || '{}')
const STORAGE_KEY = `branch-inventory-v1-${branchSession.branchId || 'unknown'}`
const inventories = ref([])
const loading = ref(true)
const keyword = ref('')
const filter = ref('all')
const sortBy = ref('low')
const modalItem = ref(null)
const adjustType = ref('OUT')
const adjustGram = ref(115)
const adjustMemo = ref('')
const toast = ref('')
const lastSaved = ref('방금 전')
const newOrderCount = ref(0)
// 재고 현황에서 바로 재고를 신청할 때 쓰는 상태
const requestItem = ref(null)
const requestTubs = ref(1)
const requestUrgency = ref('NORMAL')
const requestReason = ref('')
const requestError = ref('')
const requesting = ref(false)
let refreshTimer

const tabs = [
  { label: '전체', value: 'all' }, { label: '정상', value: 'normal' },
  { label: '재고 부족', value: 'low' }, { label: '품절', value: 'soldout' },
  { label: '배송 중', value: 'shipping' }
]
const adjustTypes = [{ label: '차감', value: 'OUT' }, { label: '입고', value: 'IN' }, { label: '현재량으로 변경', value: 'SET' }]
const urgencyOptions = [{ label: '여유', value: 'LOW' }, { label: '보통', value: 'NORMAL' }, { label: '긴급', value: 'HIGH' }]
const quickSizes = [{ name: '싱글R', g: 115 }, { name: '싱글K', g: 145 }, { name: '더블J', g: 150 }, { name: '파인트', g: 336 }, { name: '쿼터', g: 643 }, { name: '1통', g: 3000 }]
const fallbackFlavors = ['엄마는 외계인', '아몬드 봉봉', '민트 초콜릿 칩', '뉴욕 치즈케이크', '체리쥬빌레', '바람과 함께 사라지다', '레인보우 샤베트', '슈팅스타', '베리베리 스트로베리', '31요거트', '오레오 쿠키 앤 크림', '초콜릿 무스']

const totalGram = computed(() => inventories.value.reduce((sum, item) => sum + item.remainingG, 0))
const totalTubs = computed(() => (totalGram.value / 3000).toFixed(1))
const lowCount = computed(() => inventories.value.filter(item => item.remainingG <= 3000).length)
const shippingCount = computed(() => inventories.value.filter(item => activeRequest(item)).length)
const previewGram = computed(() => {
  if (!modalItem.value) return 0
  if (adjustType.value === 'IN') return modalItem.value.remainingG + Number(adjustGram.value || 0)
  if (adjustType.value === 'SET') return Math.max(0, Number(adjustGram.value || 0))
  return Math.max(0, modalItem.value.remainingG - Number(adjustGram.value || 0))
})

const filteredInventories = computed(() => {
  let rows = inventories.value.filter(item => item.name.toLowerCase().includes(keyword.value.trim().toLowerCase()))
  if (filter.value === 'normal') rows = rows.filter(item => item.remainingG > 3000)
  if (filter.value === 'low') rows = rows.filter(item => item.remainingG > 0 && item.remainingG <= 3000)
  if (filter.value === 'soldout') rows = rows.filter(item => item.remainingG === 0)
  if (filter.value === 'shipping') rows = rows.filter(item => activeRequest(item))
  return [...rows].sort((a, b) => sortBy.value === 'name' ? a.name.localeCompare(b.name, 'ko') : sortBy.value === 'recent' ? b.updatedAt - a.updatedAt : a.remainingG - b.remainingG)
})

function statusOf(item) {
  if (item.remainingG <= 0) return { key: 'soldout', label: '품절' }
  if (item.remainingG <= 3000) return { key: 'low', label: '부족' }
  return { key: 'normal', label: '정상' }
}
function activeRequest(item) { return ['PENDING', 'APPROVED', 'PREPARING', 'SHIPPING', 'DELIVERED'].includes(item.requestStatus) }
function formatGram(value) { return `${Math.round(value).toLocaleString('ko-KR')}g` }
function tabCount(value) {
  if (value === 'all') return inventories.value.length
  if (value === 'normal') return inventories.value.filter(i => i.remainingG > 3000).length
  if (value === 'low') return inventories.value.filter(i => i.remainingG > 0 && i.remainingG <= 3000).length
  if (value === 'soldout') return inventories.value.filter(i => i.remainingG === 0).length
  return inventories.value.filter(activeRequest).length
}
function save(message) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(inventories.value))
  lastSaved.value = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  toast.value = message
  window.clearTimeout(save.timer)
  save.timer = window.setTimeout(() => { toast.value = '' }, 2600)
}
function autoRequestStock(item) {
  if (activeRequest(item)) return
  item.requestStatus = 'PENDING'
  item.requestTubs = 1
  item.expectedDate = new Date(Date.now() + 3 * 86400000).toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' })
  item.updatedAt = Date.now()
  save(`${item.name} 1통이 본점에 자동 신청되었습니다.`)
  window.setTimeout(() => {
    if (item.requestStatus === 'PENDING') { item.requestStatus = 'SHIPPING'; save(`${item.name} 재고가 배송을 시작했습니다.`) }
  }, 900)
}
async function completeDelivery(item) {
  await http.post(`/inventory/${item.inventoryId}/receive`)
  toast.value = `${item.name} 입고를 완료했습니다.`
  await loadServerInventory()
}
// 재고가 부족한 걸 확인한 자리에서 바로 신청할 수 있게, 해당 맛을 미리 채운 채로 창을 연다.
function openRequest(item) {
  requestItem.value = item
  // 안전재고(1통) 아래로 떨어졌으면 기본 2통, 아니면 1통을 제안한다.
  requestTubs.value = item.remainingG <= 3000 ? 2 : 1
  requestUrgency.value = item.remainingG <= 3000 ? 'HIGH' : 'NORMAL'
  requestReason.value = ''
  requestError.value = ''
}
function closeRequest() { requestItem.value = null }

async function submitRequest() {
  const item = requestItem.value
  if (!item) return
  if (!requestTubs.value || requestTubs.value < 1) {
    requestError.value = '신청 수량은 1통 이상이어야 합니다.'
    return
  }
  requesting.value = true
  requestError.value = ''
  try {
    await http.post('/stock-requests', {
      requestReason: requestReason.value,
      urgency: requestUrgency.value,
      items: [{ flavorId: item.id, requestedQuantity: requestTubs.value }]
    })
    closeRequest()
    toast.value = `${item.name} ${requestTubs.value}통을 본점에 신청했습니다.`
    await loadServerInventory()
  } catch (e) {
    requestError.value = e.response?.data?.message || '재고 신청에 실패했습니다.'
  } finally {
    requesting.value = false
  }
}

function openAdjust(item) { modalItem.value = item; adjustType.value = 'OUT'; adjustGram.value = 115; adjustMemo.value = '' }
function closeModal() { modalItem.value = null }
async function applyAdjustment() {
  const item = modalItem.value
  if (!item || Number(adjustGram.value) <= 0) return
  await http.patch(`/inventory/${item.inventoryId}`, { type: adjustType.value, grams: String(adjustGram.value) })
  toast.value = `${item.name} 재고를 반영했습니다.`
  closeModal()
  await loadServerInventory()
}

function applyAutomaticRequests() {
  inventories.value
    .filter(item => item.remainingG <= 3000 && !activeRequest(item))
    .forEach(item => autoRequestStock(item))
}

async function loadServerInventory() {
  try {
    const [inventoryResponse, orderResponse] = await Promise.all([http.get('/inventory'), http.get('/orders')])
    inventories.value = inventoryResponse.data.map(item => ({
      id: item.flavorId, inventoryId: item.inventoryId, name: item.flavorName, imageUrl: item.imageUrl || '', remainingG: item.remainingGrams,
      requestStatus: item.requestStatus, requestTubs: item.requestStatus ? 1 : 0,
      expectedDate: item.expectedArrivalAt ? new Date(item.expectedArrivalAt).toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' }) : '',
      updatedAt: item.updatedAt ? new Date(item.updatedAt).getTime() : Date.now()
    }))
    newOrderCount.value = orderResponse.data.filter(order => order.status === 'PAID').length
    lastSaved.value = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  } catch (requestError) { console.error(requestError) }
  finally { loading.value = false }
}
onMounted(() => { loadServerInventory(); refreshTimer = window.setInterval(loadServerInventory, 3000) })
onBeforeUnmount(() => window.clearInterval(refreshTimer))
</script>

<style scoped>
:global(body) { background: #f7f7fa; color: #252329; }
button, input, select { font: inherit; }
button { cursor: pointer; }
.admin-shell { min-height: 100vh; }
/* 사이드바 모양은 공통 BranchSidebar에서만 관리해 다른 지점 화면과 동일하게 유지한다. */
.logo { padding: 0 13px 36px; color: #2e2930; font-size: 20px; font-weight: 900; letter-spacing: -.03em; }.logo span { display: inline-grid; width: 39px; height: 39px; margin-right: 8px; place-items: center; color: #fff; background: #ef3f91; border-radius: 12px; }
nav { display: grid; gap: 7px; }nav a { display: flex; align-items: center; gap: 13px; padding: 13px 14px; color: #746c72; border-radius: 11px; font-size: 14px; font-weight: 700; text-decoration: none; }nav a i { width: 20px; color: #aaa1a7; font-style: normal; text-align: center; }nav a.active { color: #e93685; background: #fff0f6; }nav a.active i { color: #e93685; }
.manager { display: flex; align-items: center; gap: 10px; margin-top: auto; padding: 15px 8px 0; border-top: 1px solid #f0ecef; }.avatar { display: grid; flex: 0 0 38px; height: 38px; place-items: center; color: #fff; background: #2e2930; border-radius: 50%; font-weight: 800; }.manager strong,.manager span { display: block; }.manager strong { font-size: 13px; }.manager span { margin-top: 3px; color: #a19aa0; font-size: 11px; }.manager button { margin-left: auto; border: 0; background: transparent; }
.content { margin-left: 238px; padding: 38px 44px 60px; }.topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 27px; }.eyebrow { margin: 0 0 7px !important; color: #ed3c8d !important; font-size: 10px !important; font-weight: 900; letter-spacing: .14em; }.topbar h1 { margin: 0 0 7px; font-size: 30px; letter-spacing: -.05em; }.topbar p { margin: 0; color: #8b8389; font-size: 14px; }.top-actions { display: flex; align-items: center; gap: 10px; }.updated { margin-right: 8px; color: #a39ba1; font-size: 11px; }.icon-button { position: relative; width: 42px; height: 42px; border: 1px solid #e7e1e5; background: #fff; border-radius: 11px; }.icon-button b { position: absolute; top: -5px; right: -5px; display: grid; width: 19px; height: 19px; place-items: center; color: #fff; background: #ef3f91; border: 2px solid #fff; border-radius: 50%; font-size: 9px; }.primary { padding: 12px 18px; color: #fff; border: 0; background: #ef3f91; border-radius: 10px; font-size: 13px; font-weight: 800; box-shadow: 0 8px 20px rgb(239 63 145 / 20%); }
.auto-order { display: flex; align-items: center; gap: 7px; padding: 11px 14px; color: #615a5f; border: 1px solid #dcebe3; background: #f2fbf6; border-radius: 10px; font-size: 11px; font-weight: 700; }.auto-order i { width: 7px; height: 7px; background: #38a970; border-radius: 50%; box-shadow: 0 0 0 4px rgb(56 169 112 / 12%); }.auto-order strong { color: #218654; }
.summary-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 14px; margin-bottom: 20px; }.summary-grid article { display: flex; align-items: center; gap: 15px; padding: 20px; border: 1px solid #ece8eb; background: #fff; border-radius: 15px; box-shadow: 0 4px 14px rgb(55 39 47 / 3%); }.summary-icon { display: grid; flex: 0 0 43px; height: 43px; place-items: center; border-radius: 12px; font-weight: 900; }.summary-icon.pink { color: #eb3d8c; background: #fff0f6; }.summary-icon.blue { color: #4384db; background: #edf5ff; }.summary-icon.orange { color: #e7892b; background: #fff5e9; }.summary-icon.green { color: #34a56f; background: #edf9f3; }.summary-grid span,.summary-grid strong,.summary-grid small { display: block; }.summary-grid span { color: #8e868b; font-size: 11px; }.summary-grid strong { margin: 4px 0 3px; font-size: 21px; }.summary-grid small { color: #aaa2a7; font-size: 10px; }
.inventory-panel { overflow: hidden; border: 1px solid #ebe6e9; background: #fff; border-radius: 16px; box-shadow: 0 6px 20px rgb(55 39 47 / 4%); }.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 19px; border-bottom: 1px solid #eee9ec; }.tabs { display: flex; gap: 3px; }.tabs button { padding: 8px 11px; color: #8a8287; border: 0; background: transparent; border-radius: 8px; font-size: 12px; font-weight: 700; }.tabs button span { padding: 2px 5px; background: #f1eef0; border-radius: 6px; font-size: 9px; }.tabs button.active { color: #e93685; background: #fff0f6; }.tabs button.active span { background: #ffdce9; }.tools { display: flex; gap: 8px; }.search { display: flex; align-items: center; gap: 7px; width: 190px; padding: 0 10px; border: 1px solid #e4dfe2; border-radius: 9px; }.search input { width: 100%; padding: 9px 0; border: 0; outline: 0; font-size: 11px; }.tools select { padding: 0 28px 0 10px; color: #686166; border: 1px solid #e4dfe2; background: #fff; border-radius: 9px; font-size: 11px; }
.table-wrap { overflow-x: auto; }table { width: 100%; border-collapse: collapse; }th { padding: 12px 16px; color: #9d969b; background: #fbfafb; font-size: 10px; font-weight: 700; text-align: left; }td { padding: 14px 16px; border-top: 1px solid #f1edf0; font-size: 12px; vertical-align: middle; }tbody tr:hover { background: #fffbfd; }.flavor { display: flex; align-items: center; gap: 10px; min-width: 180px; }.flavor img,.flavor-placeholder { width: 42px; height: 42px; object-fit: contain; background: #fff5f9; border-radius: 11px; }.flavor-placeholder { display: grid; place-items: center; color: #ef3f91; font-weight: 900; }.flavor strong,.flavor span,.stock-number,.tub-count { display: block; }.flavor strong { margin-bottom: 4px; font-size: 12px; }.flavor span { color: #afa7ac; font-size: 9px; }.stock-number { font-size: 13px; }.tub-count { margin-top: 4px; color: #9d959a; font-size: 10px; }.progress { overflow: hidden; width: 105px; height: 6px; margin-bottom: 5px; background: #eee9ec; border-radius: 10px; }.progress span { display: block; height: 100%; border-radius: 10px; }.progress span.normal { background: #4db37e; }.progress span.low { background: #f0a13e; }.progress span.soldout { background: #e65161; }td small { color: #aaa2a7; font-size: 9px; }.status { display: inline-flex; align-items: center; gap: 6px; padding: 5px 8px; border-radius: 7px; font-size: 10px; font-weight: 800; }.status i { width: 5px; height: 5px; border-radius: 50%; }.status.normal { color: #268b5d; background: #eaf8f1; }.status.normal i { background: #39a970; }.status.low { color: #c2741d; background: #fff3e2; }.status.low i { background: #e79635; }.status.soldout { color: #c74454; background: #ffeaed; }.status.soldout i { background: #df4d5e; }.delivery strong,.delivery span { display: block; }.delivery strong { color: #397bc7; font-size: 11px; }.delivery span { margin-top: 3px; color: #91898e; font-size: 9px; }.delivery.pending strong { color: #ca7b22; }.delivery.done strong { color: #268b5d; }.muted { color: #b0a9ad; font-size: 10px; }.row-actions { display: flex; gap: 5px; }.row-actions button { white-space: nowrap; padding: 7px 9px; border: 1px solid #dfd9dd; background: #fff; border-radius: 7px; font-size: 9px; font-weight: 700; }.row-actions .request { color: #e93685; border-color: #f4b8d3; }.row-actions .receive { color: #fff; border-color: #397bc7; background: #397bc7; }.empty { padding: 70px; color: #999197; text-align: center; }
.modal-backdrop { position: fixed; inset: 0; z-index: 20; display: grid; padding: 24px; place-items: center; background: rgb(28 22 25 / 46%); backdrop-filter: blur(3px); }.modal { position: relative; width: min(480px,100%); padding: 30px; background: #fff; border-radius: 19px; box-shadow: 0 25px 70px rgb(32 17 24 / 25%); }.close { position: absolute; top: 15px; right: 16px; border: 0; background: transparent; font-size: 25px; }.modal h2 { margin: 0 0 8px; font-size: 22px; }.current { margin: 0 0 20px; color: #898187; font-size: 12px; }.type-buttons { display: grid; grid-template-columns: repeat(3,1fr); gap: 7px; margin-bottom: 16px; }.type-buttons button { padding: 10px; color: #746c71; border: 1px solid #e3dde1; background: #fff; border-radius: 9px; font-size: 11px; font-weight: 700; }.type-buttons button.active { color: #e93685; border-color: #ef82b3; background: #fff2f7; }.modal label { display: grid; gap: 7px; margin-top: 13px; color: #665f63; font-size: 11px; font-weight: 800; }.modal input { padding: 11px 12px; border: 1px solid #dfd9dd; border-radius: 9px; outline: 0; }.modal input:focus { border-color: #ef6ba7; }.quick-grams { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 7px; }.quick-grams button { padding: 6px 8px; color: #756d72; border: 1px solid #e5dfe3; background: #faf9fa; border-radius: 7px; font-size: 9px; }.preview { display: flex; align-items: center; gap: 7px; margin-top: 18px; padding: 13px; background: #faf7f9; border-radius: 10px; color: #7e767b; font-size: 11px; }.preview strong { color: #29252a; }.preview span { margin-left: auto; color: #cb751b; font-weight: 800; }.modal-actions { display: flex; justify-content: flex-end; gap: 7px; margin-top: 19px; }.modal-actions button { padding: 10px 16px; border: 1px solid #ded7dc; background: #fff; border-radius: 9px; font-size: 11px; font-weight: 800; }.modal-actions .primary { color: #fff; border-color: #ef3f91; background: #ef3f91; }.toast { position: fixed; right: 25px; bottom: 25px; z-index: 30; padding: 14px 18px; color: #fff; background: #272329; border-radius: 11px; box-shadow: 0 12px 35px rgb(24 16 20 / 25%); font-size: 12px; font-weight: 700; }
.qty { display: flex; align-items: center; gap: 6px; }
.qty button { width: 30px; height: 30px; color: #6d656b; border: 1px solid #e0dade; background: #fff; border-radius: 8px; font-size: 14px; }
.qty input { width: 60px; padding: 8px; border: 1px solid #e0dade; border-radius: 8px; font-size: 12px; text-align: center; }
.qty .unit { color: #918990; font-size: 10px; font-weight: 700; }
.form-error { margin: 14px 0 0; padding: 10px 12px; color: #c52f47; background: #ffecef; border-radius: 8px; font-size: 11px; }
.modal-actions .primary:disabled { opacity: .55; cursor: wait; }
@media (max-width: 1100px) { .content { margin-left: 238px; padding: 30px 24px; }.summary-grid { grid-template-columns: repeat(2,1fr); }.updated { display: none; } }
@media (max-width: 760px) { .content { margin-left: 0; padding: 22px 14px; }.topbar,.panel-head { align-items: flex-start; flex-direction: column; gap: 14px; }.top-actions { width: 100%; }.summary-grid { grid-template-columns: 1fr 1fr; }.tools,.search { width: 100%; }.panel-head { overflow: hidden; }.tabs { overflow-x: auto; width: 100%; }.top-actions .primary { margin-left: auto; } }
</style>
