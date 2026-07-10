<template>
  <section class="order">
    <p>단계: {{ stepLabel }}</p>

    <!-- 1단계: 매장/포장 선택 (CU-002) -->
    <div v-if="step === 'orderType'">
      <h2>매장에서 드시나요, 포장하시나요?</h2>
      <button type="button" @click="selectOrderType('DINE_IN')">매장 (Dine in)</button>
      <button type="button" @click="selectOrderType('TAKEOUT')">포장 (Takeout)</button>
    </div>

    <!-- 2단계: 상품 선택 -->
    <div v-else-if="step === 'product'">
      <h2>메뉴 선택</h2>
      <p v-if="loading">불러오는 중...</p>
      <p v-else-if="loadError">상품을 불러오지 못했습니다.</p>
      <ul v-else>
        <li v-for="product in products" :key="product.productId">
          <button type="button" @click="selectProduct(product)">
            {{ product.productName }} - {{ product.basePrice.toLocaleString() }}원
          </button>
        </li>
      </ul>
      <button v-if="cart.items.length" type="button" @click="step = 'cart'">
        장바구니 확인 ({{ cart.totalCount }})
      </button>
    </div>

    <!-- 3단계: 맛 선택 (CU-006) -->
    <div v-else-if="step === 'flavor'">
      <h2>{{ selectedProduct.productName }}</h2>

      <div v-if="selectedProduct.containerPolicy !== 'NONE'">
        <p>용기 선택</p>
        <button
          v-if="selectedProduct.containerPolicy === 'CUP_ONLY' || selectedProduct.containerPolicy === 'CUP_OR_CONE'"
          type="button"
          @click="containerType = 'CUP'"
        >
          컵{{ containerType === 'CUP' ? ' ✓' : '' }}
        </button>
        <button
          v-if="selectedProduct.containerPolicy === 'CUP_OR_CONE'"
          type="button"
          @click="containerType = 'CONE'"
        >
          콘{{ containerType === 'CONE' ? ' ✓' : '' }}
        </button>
      </div>

      <div v-if="selectedProduct.requiresFlavorSelection">
        <!-- CU-006-2: 선택 진행률 표시 -->
        <p>맛 선택 ({{ selectedFlavorIds.length }} / {{ selectedProduct.selectableFlavorCount }})</p>
        <ul>
          <li v-for="flavor in flavors" :key="flavor.flavorId">
            <button
              type="button"
              :disabled="
                !selectedFlavorIds.includes(flavor.flavorId) &&
                selectedFlavorIds.length >= selectedProduct.selectableFlavorCount
              "
              @click="toggleFlavor(flavor.flavorId)"
            >
              {{ flavor.flavorName }}{{ selectedFlavorIds.includes(flavor.flavorId) ? ' ✓' : '' }}
            </button>
          </li>
        </ul>
      </div>

      <div v-if="selectedProduct.isLarge">
        <label>
          숟가락 개수
          <input v-model.number="spoonCount" type="number" min="0" />
        </label>
        <label>
          드라이아이스 시간(분)
          <select v-model.number="dryIceMinutes">
            <option :value="null">사용 안함</option>
            <option :value="10">10분</option>
            <option :value="20">20분</option>
            <option :value="30">30분</option>
          </select>
        </label>
      </div>

      <button type="button" @click="step = 'product'">뒤로</button>
      <!-- CU-006-1: 조건 충족 전까지 담기 버튼 비활성화 -->
      <button type="button" :disabled="!canConfirmFlavor" @click="confirmAddToCart">
        {{ editingItemId ? '수정 완료' : '장바구니 담기' }}
      </button>
    </div>

    <!-- 4단계: 장바구니 확인 (CU-007) -->
    <div v-else-if="step === 'cart'">
      <h2>장바구니 확인</h2>
      <p>
        {{ cart.orderType === 'DINE_IN' ? '매장' : '포장' }} · {{ cart.totalCount }}개 ·
        {{ cart.totalAmount.toLocaleString() }}원
      </p>
      <ul>
        <li v-for="item in cart.items" :key="item.id">
          {{ item.productName }}
          <span v-if="item.flavors.length">({{ item.flavors.map((f) => f.flavorName).join(', ') }})</span>
          {{ (item.unitPrice * item.quantity).toLocaleString() }}원
          <button type="button" @click="editItem(item)">수정</button>
          <button type="button" @click="removeFromCart(item.id)">삭제</button>
        </li>
      </ul>
      <button type="button" @click="step = 'product'">메뉴 더 담기</button>
      <button type="button" @click="showPaymentJson = !showPaymentJson">결제</button>

      <pre v-if="showPaymentJson">{{ paymentJson }}</pre>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../../api/http'
import { useCartStore } from '../../stores/cart'

const cart = useCartStore()

// 이미 매장/포장을 골라둔 상태로 돌아오면(새로고침 등) 1단계를 다시 안 거치고 이어서 진행
const step = ref(cart.orderType ? 'product' : 'orderType')

const stepLabel = computed(
  () =>
    ({
      orderType: '매장/포장 선택',
      product: '상품 선택',
      flavor: '맛 선택',
      cart: '장바구니 확인'
    })[step.value]
)

const products = ref([])
const flavors = ref([])
const loading = ref(true)
const loadError = ref(false)

const selectedProduct = ref(null)
const editingItemId = ref(null)
const selectedFlavorIds = ref([])
const containerType = ref('NONE')
const spoonCount = ref(0)
const dryIceMinutes = ref(null)
const showPaymentJson = ref(false)

onMounted(async () => {
  try {
    const [productsRes, flavorsRes] = await Promise.all([http.get('/products'), http.get('/flavors')])
    products.value = productsRes.data
    flavors.value = flavorsRes.data
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

const paymentJson = computed(() =>
  JSON.stringify(
    {
      orderType: cart.orderType,
      items: cart.items,
      totalCount: cart.totalCount,
      totalAmount: cart.totalAmount
    },
    null,
    2
  )
)

const canConfirmFlavor = computed(() => {
  if (!selectedProduct.value) return false
  if (selectedProduct.value.requiresFlavorSelection) {
    return selectedFlavorIds.value.length === selectedProduct.value.selectableFlavorCount
  }
  return true
})

function selectOrderType(orderType) {
  cart.setOrderType(orderType)
  step.value = 'product'
}

function resetFlavorStepState(product) {
  selectedProduct.value = product
  selectedFlavorIds.value = []
  containerType.value = product.containerPolicy === 'NONE' ? 'NONE' : 'CUP'
  spoonCount.value = 0
  dryIceMinutes.value = null
}

function selectProduct(product) {
  editingItemId.value = null
  resetFlavorStepState(product)

  if (!product.requiresFlavorSelection && product.containerPolicy === 'NONE' && !product.isLarge) {
    // 추가 옵션이 전혀 없는 상품(예: 아메리카노)은 바로 장바구니에 담고 확인 단계로 이동
    addCurrentSelectionToCart()
    step.value = 'cart'
    return
  }

  step.value = 'flavor'
}

function editItem(item) {
  const product = products.value.find((p) => p.productId === item.productId)
  if (!product) return
  editingItemId.value = item.id
  selectedProduct.value = product
  selectedFlavorIds.value = item.flavors.map((f) => f.flavorId)
  containerType.value = item.containerType
  spoonCount.value = item.spoonCount
  dryIceMinutes.value = item.dryIceMinutes
  step.value = 'flavor'
}

function toggleFlavor(flavorId) {
  const index = selectedFlavorIds.value.indexOf(flavorId)
  if (index === -1) {
    selectedFlavorIds.value.push(flavorId)
  } else {
    selectedFlavorIds.value.splice(index, 1)
  }
}

function addCurrentSelectionToCart() {
  const selectedFlavors = selectedFlavorIds.value.map((flavorId, index) => {
    const flavor = flavors.value.find((f) => f.flavorId === flavorId)
    return { flavorId, flavorName: flavor?.flavorName ?? '', selectionOrder: index + 1, quantity: 1 }
  })

  const payload = {
    productId: selectedProduct.value.productId,
    productName: selectedProduct.value.productName,
    unitPrice: selectedProduct.value.basePrice,
    containerType: containerType.value,
    spoonCount: spoonCount.value,
    dryIceMinutes: dryIceMinutes.value,
    flavors: selectedFlavors
  }

  if (editingItemId.value) {
    cart.updateItem(editingItemId.value, payload)
  } else {
    cart.addItem(payload)
  }
}

function confirmAddToCart() {
  addCurrentSelectionToCart()
  step.value = 'cart'
}

function removeFromCart(id) {
  // CU-007-1: 삭제 전 재확인
  if (confirm('이 메뉴를 삭제하시겠습니까?')) {
    cart.removeItem(id)
  }
}
</script>
