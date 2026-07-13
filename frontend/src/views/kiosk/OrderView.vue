<template>
  <section class="order">
    <p>단계: {{ stepLabel }}</p>

    <!-- 1단계: 매장/포장 선택 (CU-002) - 팝업 형태 -->
    <div v-if="step === 'orderType'" class="modal-backdrop">
      <div class="modal">
        <h2>매장에서 드시나요, 포장하시나요?</h2>
        <p class="subtitle">Dine in or Takeout?</p>
        <button type="button" @click="selectOrderType('DINE_IN')">매장 (Dine in)</button>
        <button type="button" @click="selectOrderType('TAKEOUT')">포장 (Takeout)</button>
      </div>
    </div>

    <!-- 2단계: 카테고리 탭 + 상품 목록 (탭 클릭 시 메인 목록만 바뀜) -->
    <div v-else-if="step === 'product'">
      <!-- 상단 토글 탭 -->
      <nav class="category-tabs">
        <button
          v-for="category in categories"
          :key="category.categoryId"
          type="button"
          :class="{ active: selectedCategory?.categoryId === category.categoryId }"
          @click="selectedCategory = category"
        >
          {{ category.categoryName }}
        </button>
      </nav>

      <h2>{{ selectedCategory?.categoryName }} 메뉴 선택</h2>
      <p v-if="loading">불러오는 중...</p>
      <p v-else-if="loadError">상품을 불러오지 못했습니다.</p>
      <ul v-else>
        <li v-for="product in visibleProducts" :key="product.productId">
          <button type="button" @click="selectProduct(product)">
            {{ product.productName }} - {{ product.basePrice.toLocaleString() }}원
          </button>
        </li>
      </ul>
      <button v-if="cart.items.length" type="button" @click="step = 'cart'">
        장바구니 확인 ({{ cart.totalCount }})
      </button>
    </div>

    <!-- 4단계: 픽업일시 + 컵/콘 선택 (CU-005) -->
    <div v-else-if="step === 'pickupContainer'">
      <h2>{{ selectedProduct.productName }}</h2>

      <div v-if="showPickupTime">
        <p>픽업일시를 선택하세요</p>
        <button
          v-for="option in pickupOptions"
          :key="option.minutes"
          type="button"
          :class="{ active: pendingPickupMinutes === option.minutes }"
          @click="pendingPickupMinutes = option.minutes"
        >
          {{ option.label }}
        </button>
      </div>

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

      <button type="button" @click="step = 'product'">뒤로</button>
      <button type="button" :disabled="!canProceedPickupContainer" @click="confirmPickupContainer">다음</button>
    </div>

    <!-- 5단계: 맛 선택 (CU-006) -->
    <div v-else-if="step === 'flavor'">
      <h2>{{ selectedProduct.productName }}</h2>

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

      <button type="button" @click="step = 'pickupContainer'">뒤로</button>
      <!-- CU-006-1: 조건 충족 전까지 담기 버튼 비활성화 -->
      <button type="button" :disabled="!canConfirmFlavor" @click="confirmAddToCart">
        {{ editingItemId ? '수정 완료' : '장바구니 담기' }}
      </button>
    </div>

    <!-- 6단계: 장바구니 확인 (CU-007) -->
    <div v-else-if="step === 'cart'">
      <h2>장바구니 확인</h2>
      <p>
        {{ cart.orderType === 'DINE_IN' ? '매장' : '포장' }} · {{ cart.totalCount }}개 ·
        {{ cart.amountBeforeDiscount.toLocaleString() }}원
      </p>
      <p v-if="cart.pickupAt">픽업 예정: {{ new Date(cart.pickupAt).toLocaleString() }}</p>
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
      <button type="button" @click="step = 'customer'">결제</button>
    </div>

    <!-- 7단계: 포인트/할인 선택 (CU-008) -->
    <div v-else-if="step === 'customer'">
      <h2>포인트 적립/사용</h2>
      <label>
        휴대폰 번호
        <input v-model="mobileNumberInput" type="text" placeholder="01012345678" />
      </label>
      <button type="button" @click="lookupCustomer">조회</button>

      <p v-if="customerLookupDone && !customer">등록된 회원이 아닙니다. 포인트 없이 진행합니다.</p>
      <div v-if="customer">
        <p>{{ customer.grade }} 등급 · 보유 포인트 {{ customer.pointBalance.toLocaleString() }}P</p>
        <p>
          사용 포인트: {{ cart.usedPoints.toLocaleString() }}P
          (예상 적립 {{ estimatedEarnedPoints.toLocaleString() }}P)
        </p>
        <button type="button" @click="adjustUsedPoints(-1000)">-1000</button>
        <button type="button" @click="adjustUsedPoints(-100)">-100</button>
        <button type="button" @click="adjustUsedPoints(100)">+100</button>
        <button type="button" @click="adjustUsedPoints(1000)">+1000</button>
        <button type="button" @click="useMaxPoints">최대금액사용</button>
      </div>

      <p>결제 금액: {{ cart.totalAmount.toLocaleString() }}원</p>

      <button type="button" @click="step = 'cart'">뒤로</button>

      <!-- CU-009: 결제 실행 -->
      <div v-if="!qrInfo">
        <button type="button" :disabled="checkoutInProgress" @click="startPayment">결제</button>
        <p v-if="checkoutError">{{ checkoutError }}</p>
      </div>
      <div v-else class="modal-backdrop">
        <div class="modal">
          <p>결제 금액: {{ qrInfo.requestedAmount.toLocaleString() }}원</p>
          <img :src="qrDataUrl" alt="결제 QR코드" width="200" height="200" />
          <p>휴대폰으로 QR코드를 스캔해서 결제를 완료해주세요.</p>
          <p>상태: {{ paymentStatusLabel }}</p>
          <p v-if="paymentStatus === 'PAID'">결제가 완료되었습니다. 감사합니다!</p>
          <div v-else class="qr-actions">
            <!-- CU-009-1: 결제 실패 시 QR코드 재생성 -->
            <button type="button" @click="regenerateQr">QR코드 재생성</button>
            <button type="button" @click="testPayment" class="test-btn">결제 완료 처리 (테스트용)</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import http from '../../api/http'
import { fetchCategories, fetchProducts, fetchFlavors } from '../../services/menuService'
import { useCartStore } from '../../stores/cart'

const router = useRouter()

// 지금은 키오스크 1대 = 지점 1곳으로 가정하고 고정값 사용 (지점/로그인 붙기 전까지 임시)
const BRANCH_ID = 1

const cart = useCartStore()

// 이미 매장/포장을 골라둔 상태로 돌아오면(새로고침 등) 1단계를 다시 안 거치고 이어서 진행
const step = ref(cart.orderType ? 'product' : 'orderType')

const stepLabel = computed(
  () =>
    ({
      orderType: '매장/포장 선택',
      product: '상품 선택',
      pickupContainer: '픽업일시/용기 선택',
      flavor: '맛 선택',
      cart: '장바구니 확인',
      customer: '포인트/결제'
    })[step.value]
)

const categories = ref([])
const products = ref([])
const flavors = ref([])
const loading = ref(true)
const loadError = ref(false)

const selectedCategory = ref(null)
const selectedProduct = ref(null)
const editingItemId = ref(null)
const selectedFlavorIds = ref([])
const containerType = ref('NONE')
const spoonCount = ref(0)
const dryIceMinutes = ref(null)

const pickupOptions = [
  { minutes: 0, label: '지금 바로' },
  { minutes: 30, label: '30분 후' },
  { minutes: 60, label: '1시간 후' }
]
const pendingPickupMinutes = ref(null)

const mobileNumberInput = ref('')
const customer = ref(null)
const customerLookupDone = ref(false)

onMounted(async () => {
  try {
    const [categoriesData, productsData, flavorsData] = await Promise.all([
      fetchCategories(),
      fetchProducts(),
      fetchFlavors()
    ])
    categories.value = categoriesData
    products.value = productsData
    flavors.value = flavorsData
    selectedCategory.value = categoriesData[0] ?? null
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

// 사이즈(레귤러/대용량) 구분 없이, 선택된 카테고리의 상품을 전부 한 화면에 보여준다.
const visibleProducts = computed(() => products.value.filter((p) => p.categoryId === selectedCategory.value?.categoryId))

const showPickupTime = computed(() => cart.orderType === 'TAKEOUT' && !cart.pickupAt)

const canProceedPickupContainer = computed(() => {
  const pickupOk = !showPickupTime.value || pendingPickupMinutes.value !== null
  const containerOk = selectedProduct.value.containerPolicy === 'NONE' || containerType.value !== 'NONE'
  return pickupOk && containerOk
})

// 등급별 적립률: Friend 3% / Family 5% / VIP 8%
const EARN_RATE = { FRIEND: 0.03, FAMILY: 0.05, VIP: 0.08 }
const estimatedEarnedPoints = computed(() => {
  if (!customer.value) return 0
  return Math.floor(cart.totalAmount * (EARN_RATE[customer.value.grade] ?? 0))
})

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
  pendingPickupMinutes.value = null
}

function selectProduct(product) {
  editingItemId.value = null
  resetFlavorStepState(product)

  if (showPickupTime.value || product.containerPolicy !== 'NONE') {
    step.value = 'pickupContainer'
    return
  }
  proceedPastPickupContainer()
}

function confirmPickupContainer() {
  if (showPickupTime.value) {
    cart.setPickupAt(new Date(Date.now() + pendingPickupMinutes.value * 60000).toISOString())
  }
  proceedPastPickupContainer()
}

function proceedPastPickupContainer() {
  if (!selectedProduct.value.requiresFlavorSelection && !selectedProduct.value.isLarge) {
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
  selectedCategory.value = categories.value.find((c) => c.categoryId === product.categoryId) ?? selectedCategory.value
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

async function lookupCustomer() {
  customerLookupDone.value = false
  customer.value = null
  if (!mobileNumberInput.value) return
  try {
    const { data } = await http.get(`/customers/${mobileNumberInput.value}`)
    customer.value = data
    cart.setCustomer(mobileNumberInput.value)
  } catch (e) {
    customer.value = null
  } finally {
    customerLookupDone.value = true
  }
}

function adjustUsedPoints(delta) {
  if (!customer.value) return
  const next = Math.min(customer.value.pointBalance, cart.amountBeforeDiscount, Math.max(0, cart.usedPoints + delta))
  cart.setUsedPoints(next)
}

function useMaxPoints() {
  if (!customer.value) return
  cart.setUsedPoints(Math.min(customer.value.pointBalance, cart.amountBeforeDiscount))
}

const checkoutInProgress = ref(false)
const checkoutError = ref('')
const orderId = ref(null)
const qrInfo = ref(null)
const qrDataUrl = ref(null)
const paymentStatus = ref(null)
let pollTimer = null

const paymentStatusLabel = computed(
  () =>
    ({
      QR_CREATED: '스캔 대기 중',
      PAID: '결제 완료',
      FAILED: '결제 실패',
      EXPIRED: '유효시간 만료',
      CANCELLED: '취소됨'
    })[paymentStatus.value] ?? paymentStatus.value
)

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPolling(qrToken) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const { data } = await http.get(`/payments/${qrToken}`)
      paymentStatus.value = data.paymentStatus
      if (data.paymentStatus === 'PAID') {
        stopPolling()
        // 결제 완료 화면을 잠시 보여준 뒤 다음 고객을 위해 광고 화면으로 복귀
        setTimeout(() => {
          cart.clear()
          router.push('/')
        }, 3000)
      }
    } catch (e) {
      // 네트워크 일시 오류는 무시하고 다음 폴링에서 재시도
    }
  }, 2000)
}

async function requestQr() {
  const { data } = await http.post('/payments/qr', { orderId: orderId.value })
  qrInfo.value = data
  paymentStatus.value = 'QR_CREATED'
  qrDataUrl.value = await QRCode.toDataURL(`${window.location.origin}/pay/${data.qrToken}`)
  startPolling(data.qrToken)
}

async function startPayment() {
  checkoutError.value = ''
  checkoutInProgress.value = true
  try {
    const { data } = await http.post('/orders/checkout', {
      branchId: BRANCH_ID,
      orderType: cart.orderType,
      pickupAt: cart.pickupAt,
      customerMobileNumber: cart.customerMobileNumber,
      usedPoints: cart.usedPoints,
      language: 'ko',
      items: cart.items.map((item) => ({
        productId: item.productId,
        containerType: item.containerType,
        spoonCount: item.spoonCount,
        dryIceMinutes: item.dryIceMinutes,
        flavorIds: item.flavors.map((f) => f.flavorId)
      }))
    })
    orderId.value = data.orderId
    await requestQr()
  } catch (e) {
    checkoutError.value = e.response?.data?.message ?? '결제 요청에 실패했습니다.'
  } finally {
    checkoutInProgress.value = false
  }
}

// CU-009-1: 결제 실패(QR 만료 등) 시 QR코드를 재생성하여 다시 시도
async function regenerateQr() {
  await requestQr()
}

async function testPayment() {
  if (!qrInfo.value) return
  try {
    await http.post(`/payments/${qrInfo.value.qrToken}/confirm`)
  } catch (e) {
    console.error('테스트 결제 처리 실패', e)
  }
}

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
}

.subtitle {
  color: #666;
}

.category-tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}

button.active {
  background: #333;
  color: #fff;
}

.qr-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1rem;
}

.test-btn {
  background-color: #ff9800;
  color: white;
  border: none;
}
</style>
