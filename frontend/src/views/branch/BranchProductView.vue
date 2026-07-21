<template>
  <div class="shell">
    <BranchSidebar active="products" />
    <main>
      <header>
        <div>
          <h1>상품 관리</h1>
          <p>우리 지점 키오스크에 노출할 상품을 설정합니다. (키오스크에 즉시 반영됩니다)</p>
        </div>
      </header>

      <div class="list-card">
        <div class="list-head">
          <h2>상품 노출 설정</h2>
          <label class="search">
            <span>⌕</span>
            <input v-model="keyword" placeholder="상품명 검색">
          </label>
        </div>

        <div v-if="loading" class="empty">상품을 불러오는 중입니다...</div>
        <div v-else-if="!filteredProducts.length" class="empty">검색 결과가 없습니다.</div>
        <table v-else>
          <thead>
            <tr>
              <th>상품 정보</th>
              <th>카테고리</th>
              <th>가격</th>
              <th>키오스크 노출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in filteredProducts" :key="product.productId">
              <td>
                <div class="entity-cell">
                  <img v-if="product.imageUrl" :src="product.imageUrl" alt="">
                  <span v-else class="thumb-placeholder">{{ product.productName.slice(0, 1) }}</span>
                  <strong>{{ product.productName }}</strong>
                </div>
              </td>
              <td>{{ product.categoryName || '-' }}</td>
              <td>₩{{ product.basePrice.toLocaleString() }}</td>
              <td>
                <label class="toggle-switch">
                  <input type="checkbox" v-model="product.isVisible" @change="toggleVisibility(product)">
                  <span class="slider"></span>
                  <span class="label-text">{{ product.isVisible ? '노출' : '숨김' }}</span>
                </label>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const products = ref([])
const loading = ref(true)
const keyword = ref('')

async function loadProducts() {
  loading.value = true
  try {
    const { data } = await http.get('/products')
    products.value = data
  } catch (e) {
    console.error('상품 목록 조회 실패', e)
    alert('상품 목록을 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

async function toggleVisibility(product) {
  try {
    const { data } = await http.patch(`/products/${product.productId}/visibility`, {
      isVisible: product.isVisible
    })
    product.isVisible = data.isVisible
  } catch (e) {
    console.error('노출 상태 변경 실패', e)
    alert('노출 상태 변경에 실패했습니다.')
    // 실패 시 되돌리기
    product.isVisible = !product.isVisible
  }
}

const filteredProducts = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return products.value
  return products.value.filter(p => p.productName.toLowerCase().includes(word))
})

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.shell { min-height: 100vh; color: #1f2938; background: #f3f6fa; }
main { margin-left: 238px; padding: 32px 34px 55px; }
header { margin-bottom: 24px; }
h1 { margin: 0; font-size: 27px; }
header p { margin: 7px 0 0; color: #7f8997; font-size: 13px; }

.list-card {
  background: #fff;
  border: 1px solid #e5e9ef;
  border-radius: 16px;
  box-shadow: 0 3px 8px rgb(34 48 68 / 3%);
  overflow: hidden;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 19px 24px;
  border-bottom: 1px solid #e9edf2;
}
.list-head h2 { margin: 0; font-size: 16px; font-weight: 700; color: #1f2938; }

.search {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 240px;
  padding: 0 12px;
  border: 1px solid #dfe3e9;
  border-radius: 8px;
  background: #f9fbfd;
}
.search span { color: #8c95a2; font-size: 14px; }
.search input {
  width: 100%;
  padding: 10px 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 13px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
th {
  padding: 14px 24px;
  color: #8c95a2;
  text-align: left;
  font-weight: 700;
  border-bottom: 1px solid #e9edf2;
  background: #fdfdfe;
}
td {
  padding: 14px 24px;
  border-bottom: 1px solid #f1f3f7;
  vertical-align: middle;
}
tr:hover td { background: #fcfcfd; }
tr:last-child td { border-bottom: 0; }

.entity-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.entity-cell img, .thumb-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
}
.thumb-placeholder {
  display: grid;
  place-items: center;
  color: #6568ee;
  background: #eef0ff;
  font-weight: 800;
  font-size: 16px;
}
.entity-cell strong { font-size: 14px; color: #1f2938; }

.empty {
  padding: 60px;
  color: #8c95a2;
  text-align: center;
  font-size: 14px;
}

/* Toggle Switch Styles */
.toggle-switch {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.toggle-switch input {
  display: none;
}
.slider {
  position: relative;
  width: 44px;
  height: 24px;
  background-color: #d1d5db;
  border-radius: 24px;
  transition: .3s;
}
.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: .3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
input:checked + .slider {
  background-color: #0b9654;
}
input:checked + .slider:before {
  transform: translateX(20px);
}
.label-text {
  font-size: 13px;
  font-weight: 700;
  color: #4b5563;
  width: 28px;
}
input:checked ~ .label-text {
  color: #0b9654;
}

@media (max-width: 760px) {
  main { margin-left: 0; padding: 20px; }
  table { display: block; overflow-x: auto; }
}
</style>
