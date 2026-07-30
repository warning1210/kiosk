<template>
  <main class="page">
    <AdminSidebar active="products" />

    <section class="content">
      <AdminPageHeader title="상품 관리" subtitle="키오스크에 노출되는 상품(용기/가격)과 맛을 등록하고 관리할 수 있습니다." />

      <div class="entity-tabs">
        <button type="button" :class="{ active: entityTab === 'product' }" @click="entityTab = 'product'">상품</button>
        <button type="button" :class="{ active: entityTab === 'flavor' }" @click="entityTab = 'flavor'">맛</button>
      </div>

      <div class="summary">
        <AdminStatCard icon="▦" label="전체" :value="`${currentList.length}개`" />
        <AdminStatCard icon="✓" label="판매중" :value="`${statusCount('ON_SALE')}개`" tone="green" />
        <AdminStatCard icon="!" label="품절" :value="`${statusCount('SOLD_OUT')}개`" tone="orange" />
        <AdminStatCard icon="✕" label="단종" :value="`${statusCount('DISCONTINUED')}개`" tone="pink" />
      </div>

      <!-- 상품 등록/수정 폼 -->
      <section v-if="entityTab === 'product'" class="form-card">
        <h2>{{ editingProductId ? '상품 수정' : '새 상품 등록' }}</h2>
        <form class="entity-form" @submit.prevent="submitProduct">
          <div class="image-field">
            <label class="image-upload">
              <img v-if="productForm.imageUrl" :src="productForm.imageUrl" alt="">
              <span v-else>이미지를 선택해주세요<br>PNG, JPG, JPEG (최대 5MB)</span>
              <input type="file" accept="image/png,image/jpeg" @change="e => handleImageUpload(e, productForm, 'product')">
            </label>
            <span v-if="productUploading" class="uploading">업로드 중...</span>
          </div>
          <div class="fields">
            <input v-model.trim="productForm.productName" placeholder="상품명">
            <select v-model="productForm.categoryId">
              <option value="">카테고리 없음</option>
              <option v-for="c in categories" :key="c.categoryId" :value="c.categoryId">{{ c.categoryName }}</option>
            </select>
            <input v-model.number="productForm.basePrice" type="number" min="0" placeholder="가격(원)">
            <select v-model="productForm.containerPolicy">
              <option value="NONE">용기 없음</option>
              <option value="CUP_ONLY">컵만</option>
              <option value="CUP_OR_CONE">컵/콘 선택</option>
            </select>
            <select v-model="productForm.saleStatus">
              <option value="ON_SALE">판매중</option>
              <option value="SOLD_OUT">품절</option>
              <option value="DISCONTINUED">단종</option>
            </select>
            <textarea v-model.trim="productForm.description" placeholder="상품 설명" rows="2"></textarea>
            <div class="checkbox-row">
              <label><input v-model="productForm.requiresFlavorSelection" type="checkbox"> 맛 선택 필요</label>
              <input v-if="productForm.requiresFlavorSelection" v-model.number="productForm.selectableFlavorCount" type="number" min="1" placeholder="선택 가능 맛 개수">
              <label><input v-model="productForm.isLarge" type="checkbox"> 라지 사이즈</label>
              <label><input v-model="productForm.isNew" type="checkbox"> 신제품</label>
              <label class="visible-toggle"><input v-model="productForm.isVisible" type="checkbox"> 키오스크에 노출</label>
            </div>
            <div class="form-actions">
              <button v-if="editingProductId" type="button" class="cancel" @click="cancelEditProduct">취소</button>
              <button :disabled="productSaving" type="submit">{{ productSaving ? '저장 중' : editingProductId ? '수정 완료' : '상품 등록' }}</button>
            </div>
          </div>
        </form>
        <p v-if="productFormError" class="alert">{{ productFormError }}</p>
      </section>

      <!-- 맛 등록/수정 폼 -->
      <section v-else class="form-card">
        <h2>{{ editingFlavorId ? '맛 수정' : '새 맛 등록' }}</h2>
        <form class="entity-form" @submit.prevent="submitFlavor">
          <div class="image-field">
            <label class="image-upload">
              <img v-if="flavorForm.imageUrl" :src="flavorForm.imageUrl" alt="">
              <span v-else>이미지를 선택해주세요<br>PNG, JPG, JPEG (최대 5MB)</span>
              <input type="file" accept="image/png,image/jpeg" @change="e => handleImageUpload(e, flavorForm, 'flavor')">
            </label>
            <span v-if="flavorUploading" class="uploading">업로드 중...</span>
          </div>
          <div class="fields">
            <input v-model.trim="flavorForm.flavorName" placeholder="맛 이름">
            <select v-model="flavorForm.categoryId">
              <option value="">카테고리 없음</option>
              <option v-for="c in categories" :key="c.categoryId" :value="c.categoryId">{{ c.categoryName }}</option>
            </select>
            <select v-model="flavorForm.saleStatus">
              <option value="ON_SALE">판매중</option>
              <option value="SOLD_OUT">품절</option>
              <option value="DISCONTINUED">단종</option>
            </select>
            <input v-model.trim="flavorForm.allergyInfo" placeholder="알레르기 정보">
            <textarea v-model.trim="flavorForm.description" placeholder="맛 설명" rows="2"></textarea>
            <div class="checkbox-row">
              <label class="visible-toggle"><input v-model="flavorForm.isVisible" type="checkbox"> 키오스크에 노출</label>
            </div>
            <div class="form-actions">
              <button v-if="editingFlavorId" type="button" class="cancel" @click="cancelEditFlavor">취소</button>
              <button :disabled="flavorSaving" type="submit">{{ flavorSaving ? '저장 중' : editingFlavorId ? '수정 완료' : '맛 등록' }}</button>
            </div>
          </div>
        </form>
        <p v-if="flavorFormError" class="alert">{{ flavorFormError }}</p>
      </section>

      <!-- 목록 -->
      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>{{ entityTab === 'product' ? '상품 목록' : '맛 목록' }}</h2>
            <span>{{ loading ? '불러오는 중' : `${filteredList.length}개` }}</span>
          </div>
          <label class="search"><span>⌕</span><input v-model="keyword" :placeholder="entityTab === 'product' ? '상품명 검색' : '맛 이름 검색'"></label>
        </div>

        <div class="tabs">
          <button v-for="tab in statusTabs" :key="tab.value" :class="{ active: statusFilter === tab.value }" type="button" @click="statusFilter = tab.value">
            {{ tab.label }} <span>{{ tab.value === '' ? currentList.length : statusCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!loading && !pagedList.length" class="empty">등록된 항목이 없습니다.</div>
        <table v-else-if="entityTab === 'product'">
          <thead><tr><th>상품</th><th>카테고리</th><th>가격</th><th>상태</th><th>노출</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="item in pagedList" :key="item.productId">
              <td>
                <div class="entity-cell">
                  <img v-if="item.imageUrl" :src="item.imageUrl" alt="">
                  <span v-else class="thumb-placeholder">{{ item.productName.slice(0, 1) }}</span>
                  <strong>{{ item.productName }}</strong>
                </div>
              </td>
              <td>{{ item.categoryName || '-' }}</td>
              <td>₩{{ item.basePrice.toLocaleString() }}</td>
              <td><span class="status" :class="item.saleStatus">{{ statusLabel(item.saleStatus) }}</span></td>
              <td>{{ item.isVisible ? '노출' : '숨김' }}</td>
              <td><button class="edit" type="button" @click="startEditProduct(item)">수정</button></td>
            </tr>
          </tbody>
        </table>
        <table v-else>
          <thead><tr><th>맛</th><th>카테고리</th><th>알레르기 정보</th><th>상태</th><th>노출</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="item in pagedList" :key="item.flavorId">
              <td>
                <div class="entity-cell">
                  <img v-if="item.imageUrl" :src="item.imageUrl" alt="">
                  <span v-else class="thumb-placeholder">{{ item.flavorName.slice(0, 1) }}</span>
                  <strong>{{ item.flavorName }}</strong>
                </div>
              </td>
              <td>{{ item.categoryName || '-' }}</td>
              <td>{{ item.allergyInfo || '-' }}</td>
              <td><span class="status" :class="item.saleStatus">{{ statusLabel(item.saleStatus) }}</span></td>
              <td>{{ item.isVisible ? '노출' : '숨김' }}</td>
              <td><button class="edit" type="button" @click="startEditFlavor(item)">수정</button></td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredList.length }}개 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredList.length" :page-size="pageSize" />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024

const entityTab = ref('product')
const categories = ref([])
const products = ref([])
const flavors = ref([])
const loading = ref(true)

const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 6

const statusTabs = [
  { label: '전체', value: '' }, { label: '판매중', value: 'ON_SALE' },
  { label: '품절', value: 'SOLD_OUT' }, { label: '단종', value: 'DISCONTINUED' }
]

function emptyProductForm() {
  return {
    categoryId: '', productName: '', basePrice: 0, imageUrl: '', description: '',
    requiresFlavorSelection: false, selectableFlavorCount: 0, containerPolicy: 'NONE',
    isLarge: false, isNew: false, saleStatus: 'ON_SALE', isVisible: true
  }
}
function emptyFlavorForm() {
  return { categoryId: '', flavorName: '', imageUrl: '', description: '', allergyInfo: '', saleStatus: 'ON_SALE', isVisible: true }
}

const productForm = reactive(emptyProductForm())
const flavorForm = reactive(emptyFlavorForm())
const editingProductId = ref(null)
const editingFlavorId = ref(null)
const productSaving = ref(false)
const flavorSaving = ref(false)
const productFormError = ref('')
const flavorFormError = ref('')
const productUploading = ref(false)
const flavorUploading = ref(false)

const currentList = computed(() => entityTab.value === 'product' ? products.value : flavors.value)

watch(entityTab, () => {
  keyword.value = ''
  statusFilter.value = ''
  page.value = 1
})

onMounted(() => {
  loadCategories()
  loadProducts()
  loadFlavors()
})

async function loadCategories() {
  try {
    categories.value = (await http.get('/hq/categories')).data
  } catch {
    categories.value = []
  }
}

async function loadProducts() {
  loading.value = true
  try {
    products.value = (await http.get('/hq/products')).data
  } catch (e) {
    productFormError.value = e.response?.data?.message || '상품 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadFlavors() {
  try {
    flavors.value = (await http.get('/hq/flavors')).data
  } catch (e) {
    flavorFormError.value = e.response?.data?.message || '맛 목록을 불러오지 못했습니다.'
  }
}

async function handleImageUpload(event, form, kind) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > MAX_IMAGE_BYTES) {
    const message = '이미지는 5MB 이하만 업로드할 수 있습니다.'
    if (kind === 'product') productFormError.value = message
    else flavorFormError.value = message
    return
  }

  const uploading = kind === 'product' ? productUploading : flavorUploading
  uploading.value = true
  try {
    const body = new FormData()
    body.append('file', file)
    const { data } = await http.post('/hq/uploads', body)
    form.imageUrl = data.url
  } catch (e) {
    const message = e.response?.data?.message || '이미지 업로드에 실패했습니다.'
    if (kind === 'product') productFormError.value = message
    else flavorFormError.value = message
  } finally {
    uploading.value = false
  }
}

async function submitProduct() {
  productSaving.value = true
  productFormError.value = ''
  try {
    const payload = { ...productForm, categoryId: productForm.categoryId || null }
    if (editingProductId.value) {
      const { data } = await http.put(`/hq/products/${editingProductId.value}`, payload)
      const index = products.value.findIndex(p => p.productId === editingProductId.value)
      if (index !== -1) products.value[index] = data
    } else {
      const { data } = await http.post('/hq/products', payload)
      products.value.unshift(data)
    }
    cancelEditProduct()
  } catch (e) {
    const data = e.response?.data
    if (data && data.message) {
      productFormError.value = data.message
    } else if (data && typeof data === 'object') {
      // 백엔드 BindingResult에서 Map<String, String> 형태로 에러를 반환할 때
      productFormError.value = Object.values(data).join(', ')
    } else {
      productFormError.value = '상품을 저장하지 못했습니다.'
    }
  } finally {
    productSaving.value = false
  }
}

function startEditProduct(item) {
  editingProductId.value = item.productId
  Object.assign(productForm, {
    categoryId: item.categoryId || '', productName: item.productName, basePrice: item.basePrice,
    imageUrl: item.imageUrl || '', description: item.description || '',
    requiresFlavorSelection: !!item.requiresFlavorSelection, selectableFlavorCount: item.selectableFlavorCount || 0,
    containerPolicy: item.containerPolicy, isLarge: !!item.isLarge, isNew: !!item.isNew,
    saleStatus: item.saleStatus, isVisible: !!item.isVisible
  })
  productFormError.value = ''
}
function cancelEditProduct() {
  editingProductId.value = null
  Object.assign(productForm, emptyProductForm())
}

async function submitFlavor() {
  flavorSaving.value = true
  flavorFormError.value = ''
  try {
    const payload = { ...flavorForm, categoryId: flavorForm.categoryId || null }
    if (editingFlavorId.value) {
      const { data } = await http.put(`/hq/flavors/${editingFlavorId.value}`, payload)
      const index = flavors.value.findIndex(f => f.flavorId === editingFlavorId.value)
      if (index !== -1) flavors.value[index] = data
    } else {
      const { data } = await http.post('/hq/flavors', payload)
      flavors.value.unshift(data)
    }
    cancelEditFlavor()
  } catch (e) {
    const data = e.response?.data
    if (data && data.message) {
      flavorFormError.value = data.message
    } else if (data && typeof data === 'object') {
      flavorFormError.value = Object.values(data).join(', ')
    } else {
      flavorFormError.value = '맛을 저장하지 못했습니다.'
    }
  } finally {
    flavorSaving.value = false
  }
}

function startEditFlavor(item) {
  editingFlavorId.value = item.flavorId
  Object.assign(flavorForm, {
    categoryId: item.categoryId || '', flavorName: item.flavorName, imageUrl: item.imageUrl || '',
    description: item.description || '', allergyInfo: item.allergyInfo || '',
    saleStatus: item.saleStatus, isVisible: !!item.isVisible
  })
  flavorFormError.value = ''
}
function cancelEditFlavor() {
  editingFlavorId.value = null
  Object.assign(flavorForm, emptyFlavorForm())
}

function statusCount(status) {
  return currentList.value.filter(item => item.saleStatus === status).length
}
function statusLabel(status) {
  return { ON_SALE: '판매중', SOLD_OUT: '품절', DISCONTINUED: '단종' }[status] || status
}

const filteredList = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  const nameOf = item => entityTab.value === 'product' ? item.productName : item.flavorName
  return currentList.value.filter(item =>
    (!statusFilter.value || item.saleStatus === statusFilter.value) &&
    (!word || nameOf(item).toLowerCase().includes(word))
  )
})
const pageStart = computed(() => filteredList.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredList.value.length))
const pagedList = computed(() => filteredList.value.slice((page.value - 1) * pageSize, page.value * pageSize))
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.entity-tabs{display:flex;gap:6px;margin-top:22px}
.entity-tabs button{padding:10px 18px;color:#697487;border:1px solid #e2e5ea;background:#fff;border-radius:9px;font-size:12px;font-weight:800;cursor:pointer}
.entity-tabs button.active{color:#fff;border-color:#5f63ee;background:#5f63ee}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:16px 0 20px}
.form-card{margin-bottom:20px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}
.form-card h2{margin:0 0 12px;font-size:14px}
.entity-form{display:flex;gap:18px}
.image-field{display:grid;gap:8px;flex:0 0 180px}
.image-upload{position:relative;display:grid;place-items:center;width:180px;height:150px;overflow:hidden;padding:10px;color:#9aa2ad;border:1px dashed #d7dbe3;border-radius:12px;background:#fafbfd;font-size:10px;text-align:center;cursor:pointer}
.image-upload img{width:100%;height:100%;object-fit:cover;border-radius:9px}
.image-upload input{position:absolute;inset:0;opacity:0;cursor:pointer}
.uploading{color:#5960e9;font-size:10px;font-weight:700}
.fields{display:grid;flex:1;grid-template-columns:1fr 1fr;gap:8px}
.fields textarea{grid-column:1/-1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px;font-family:inherit;resize:vertical}
.fields input,.fields select{padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}
.checkbox-row{display:flex;flex-wrap:wrap;align-items:center;gap:14px;grid-column:1/-1;padding:4px 0;font-size:11px;font-weight:700;color:#4e5868}
.checkbox-row label{display:flex;align-items:center;gap:6px}
.checkbox-row input[type=number]{width:150px;padding:8px 10px;border:1px solid #dfe3e9;border-radius:7px;font-size:11px}
.visible-toggle{margin-left:auto}
.form-actions{display:flex;justify-content:flex-end;gap:8px;grid-column:1/-1}
.form-actions button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}
.form-actions button:disabled{opacity:.55}
.form-actions .cancel{color:#697487;background:#eef0f3}
.alert{margin-top:10px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.tabs{display:flex;gap:3px;padding:12px 22px 0}
.tabs button{padding:8px 11px;color:#697487;border:0;background:transparent;border-radius:8px;font-size:12px;font-weight:700;cursor:pointer}
.tabs button span{padding:2px 5px;background:#eef0f3;border-radius:6px;font-size:9px}
.tabs button.active{color:#5f63ee;background:#eef0ff}.tabs button.active span{background:#dbdefc}
table{width:100%;border-collapse:collapse;font-size:11px;margin-top:8px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:12px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
.entity-cell{display:flex;align-items:center;gap:10px}
.entity-cell img,.thumb-placeholder{width:36px;height:36px;border-radius:9px;object-fit:cover}
.thumb-placeholder{display:grid;place-items:center;color:#5f63ee;background:#eef0ff;font-weight:900}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.ON_SALE{color:#0b9654;background:#e2f8ec}.status.SOLD_OUT{color:#d57d00;background:#fff3d6}.status.DISCONTINUED{color:#c63750;background:#ffe8ed}
.edit{padding:7px 10px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.entity-form{flex-direction:column}.image-field{flex:none;width:100%}.image-upload{width:100%}.fields{grid-template-columns:1fr}table{display:block;overflow-x:auto}}
</style>
