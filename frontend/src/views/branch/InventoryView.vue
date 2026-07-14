<template>
  <section>
    <!-- 페이지 설명과, 특정 맛을 고르지 않은 빈 재고신청 모달 진입 버튼 -->
    <header class="page-header">
      <div>
        <h2>재고 현황</h2>
        <p>재고가 적은 순으로 정렬 · 안전재고 이하 자동 알림</p>
      </div>
      <button type="button" class="primary" @click="openModal(null)">+ 재고 신청</button>
    </header>

    <!-- v-model로 검색어를 보관하고 Enter 또는 검색 버튼을 눌렀을 때 서버 목록을 다시 조회한다. -->
    <div class="toolbar">
      <input v-model="keyword" type="text" placeholder="상품명 검색..." @keyup.enter="load" />
      <button type="button" class="search" @click="load">검색</button>
    </div>

    <!-- 서버에서 받은 목록을 computed로 세어 품절과 부족 재고를 즉시 요약한다. -->
    <p v-if="soldOutCount > 0" class="banner danger">
      품절 {{ soldOutCount }}건 — 키오스크에서 자동 제외됨
    </p>
    <p v-if="lowCount > 0" class="banner warning">
      안전재고 이하 {{ lowCount }}건 — 재고 신청을 권장합니다
    </p>

    <!-- 조회 중, 오류, 정상 표가 동시에 나타나지 않도록 조건을 순서대로 분기한다. -->
    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="banner danger">{{ error }}</p>

    <table v-else class="inventory-table">
      <thead>
        <tr>
          <th>상품명</th>
          <th>카테고리</th>
          <th>현재고/안전</th>
          <th>상태</th>
          <th>키오스크</th>
          <th>관리</th>
        </tr>
      </thead>
      <tbody>
        <!-- branchInventoryId는 재고 행의 고유 PK이므로 목록이 바뀌어도 안정적인 key가 된다. -->
        <tr v-for="item in items" :key="item.branchInventoryId">
          <td>{{ item.flavorName }}</td>
          <td>{{ item.categoryName ?? '-' }}</td>
          <td>{{ item.currentQuantity }} / {{ item.safetyQuantity }}</td>
          <td>
            <span class="badge" :class="statusClass(item.inventoryStatus)">{{ statusLabel(item.inventoryStatus) }}</span>
          </td>
          <td>{{ item.isKioskVisible ? '노출' : '숨김' }}</td>
          <td>
            <!-- 행에서 신청하면 그 상품의 flavorId를 모달 기본 선택값으로 전달한다. -->
            <button type="button" class="link" @click="openModal(item.flavorId)">신청</button>
          </td>
        </tr>
        <tr v-if="items.length === 0">
          <td colspan="6" class="empty">등록된 재고가 없습니다</td>
        </tr>
      </tbody>
    </table>

    <!--
      modalOpen이 true일 때만 모달 컴포넌트를 만든다.
      현재 재고 목록은 선택 항목으로, presetFlavorId는 최초 선택 맛으로 내려 준다.
      close/submitted는 자식이 부모에게 보내는 이벤트이며, submitted 후에는 목록도 재조회한다.
    -->
    <StockRequestFormModal
      v-if="modalOpen"
      :inventory-items="items"
      :preset-flavor-id="presetFlavorId"
      @close="modalOpen = false"
      @submitted="onSubmitted"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchBranchInventory } from '../../api/branchInventory'
import StockRequestFormModal from './StockRequestFormModal.vue'

// 재고 API 결과와 검색/로딩/오류 상태를 각각 반응형 ref로 관리한다.
const items = ref([])
const keyword = ref('')
const loading = ref(true)
const error = ref('')

// 모달 표시 여부와 모달을 열 때 미리 선택할 맛 ID다.
const modalOpen = ref(false)
const presetFlavorId = ref(null)

// items가 바뀔 때만 상태별 개수를 다시 계산한다. 별도 카운트 상태를 두지 않아 목록과 어긋나지 않는다.
const soldOutCount = computed(() => items.value.filter((i) => i.inventoryStatus === 'SOLD_OUT').length)
const lowCount = computed(() => items.value.filter((i) => i.inventoryStatus === 'LOW').length)

// 검색 조건으로 지점 재고를 조회한다. 빈 문자열은 undefined로 바꿔 불필요한 쿼리 파라미터를 생략한다.
async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await fetchBranchInventory({ keyword: keyword.value || undefined })
  } catch (e) {
    error.value = e.response?.data?.message ?? '재고 정보를 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

// 백엔드 enum은 그대로 유지하고 화면에 필요한 한국어와 CSS 클래스만 변환한다.
function statusLabel(status) {
  if (status === 'SOLD_OUT') return '품절'
  if (status === 'LOW') return '부족'
  return '정상'
}

function statusClass(status) {
  if (status === 'SOLD_OUT') return 'danger'
  if (status === 'LOW') return 'warning'
  return 'success'
}

// flavorId가 있으면 해당 맛을 미리 선택하고, null이면 사용자가 모달에서 직접 고르게 한다.
function openModal(flavorId) {
  presetFlavorId.value = flavorId
  modalOpen.value = true
}

// 신청 성공 이벤트를 받으면 모달을 닫고 재고 목록을 다시 받아 최신 서버 상태와 맞춘다.
function onSubmitted() {
  modalOpen.value = false
  load()
}

// 이 화면이 처음 렌더링된 직후 기본 재고 목록을 한 번 조회한다.
onMounted(load)
</script>

<style scoped>
/* 페이지 제목과 재고신청 버튼을 양쪽에 배치한다. */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.page-header h2 {
  margin: 0 0 0.25rem;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  font-size: 0.8125rem;
}

.primary {
  border: none;
  background: #4f46e5;
  color: white;
  border-radius: 8px;
  padding: 0.625rem 1rem;
  cursor: pointer;
  height: fit-content;
}

/* 상품명 검색 영역 */
.toolbar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.toolbar input {
  width: 260px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.search {
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  padding: 0.5rem 0.875rem;
  cursor: pointer;
}

/* 품절과 안전재고 이하 상태를 구분하는 알림 배너 */
.banner {
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  margin-bottom: 0.5rem;
}

.banner.danger {
  background: #fef2f2;
  color: #b91c1c;
}

.banner.warning {
  background: #fffbeb;
  color: #b45309;
}

/* 재고 목록 표와 셀의 기본 모양 */
.inventory-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.inventory-table th,
.inventory-table td {
  text-align: left;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid #f1f1f4;
  font-size: 0.875rem;
}

/* 재고 상태를 색상으로도 구분하는 배지 */
.badge {
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
}

.badge.success {
  background: #ecfdf5;
  color: #059669;
}

.badge.warning {
  background: #fffbeb;
  color: #b45309;
}

.badge.danger {
  background: #fef2f2;
  color: #b91c1c;
}

/* 각 재고 행에서 신청 모달을 여는 가벼운 링크형 버튼 */
.link {
  border: none;
  background: transparent;
  color: #4f46e5;
  cursor: pointer;
  font-size: 0.8125rem;
}

/* 조회 결과가 없을 때 표시하는 표 내부 안내 */
.empty {
  text-align: center;
  color: #9ca3af;
}
</style>
