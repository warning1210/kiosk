<template>
  <section class="order">
<<<<<<< HEAD
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

    <!-- 4단계: 컵/콘 선택 (컵/콘 둘 다 가능한 상품만 이 화면을 거침) -->
    <div v-else-if="step === 'container'">
      <h2>{{ selectedProduct.productName }} - 용기 선택</h2>
      <button type="button" @click="containerType = 'CUP'">컵{{ containerType === 'CUP' ? ' ✓' : '' }}</button>
      <button type="button" @click="containerType = 'CONE'">콘{{ containerType === 'CONE' ? ' ✓' : '' }}</button>

      <button type="button" @click="step = 'product'">뒤로</button>
      <button type="button" @click="proceedPastContainer">다음</button>
    </div>

    <!-- 5단계: 맛 선택 (CU-006) -->
    <div v-else-if="step === 'flavor'">
      <h2>{{ selectedProduct.productName }}</h2>

      <div v-if="selectedProduct.requiresFlavorSelection">
        <!-- CU-006-2: 선택 진행률 표시 -->
        <p>
          맛 선택 ({{ selectedFlavorIds.length }} / {{ selectedProduct.selectableFlavorCount }})
          <span>- 같은 맛을 여러 번 선택할 수 있어요</span>
        </p>
        <ul>
          <li v-for="flavor in flavors" :key="flavor.flavorId">
            <button type="button" :disabled="!canPickMoreFlavor()" @click="toggleFlavor(flavor.flavorId)">
              {{ flavor.flavorName }}
              <span v-if="flavorSelectedCount(flavor.flavorId) > 0">(x{{ flavorSelectedCount(flavor.flavorId) }})</span>
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

      <!-- 담은 맛을 화면 하단에 실시간 표시 -->
      <footer v-if="selectedFlavorSummary.length" class="flavor-summary-bar">
        <p>선택한 맛</p>
        <ul>
          <li v-for="entry in selectedFlavorSummary" :key="entry.flavorId">
            {{ entry.flavorName }} x{{ entry.count }}
            <button type="button" @click="removeOneFlavor(entry.flavorId)">−</button>
          </li>
        </ul>
      </footer>
    </div>

    <!-- 6단계: 장바구니 확인 (CU-007) -->
    <div v-else-if="step === 'cart'">
      <h2>장바구니 확인</h2>
      <p>
        {{ cart.orderType === 'DINE_IN' ? '매장' : '포장' }} · {{ cart.totalCount }}개 ·
        {{ cart.amountBeforeDiscount.toLocaleString() }}원
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

      <p v-if="isNewMember">
        신규 회원(FRIEND 등급)으로 시작합니다. 결제 완료 시 {{ estimatedEarnedPoints.toLocaleString() }}P가 적립됩니다.
      </p>
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
          <button type="button" class="modal-close" @click="closeQrModal">×</button>
          <p>결제 금액: {{ qrInfo.requestedAmount.toLocaleString() }}원</p>
          <img :src="qrDataUrl" alt="결제 QR코드" width="200" height="200" />
          <p>휴대폰으로 QR코드를 스캔해서 결제를 완료해주세요.</p>
          <p>상태: {{ paymentStatusLabel }}</p>
          <p v-if="paymentStatus === 'PAID'">결제가 완료되었습니다. 감사합니다!</p>
          <!-- CU-009-1: 결제 실패 시 QR코드 재생성 -->
          <button v-else type="button" @click="regenerateQr">QR코드 재생성</button>

          <h3>결제 요청 JSON</h3>
          <pre class="payment-json">{{ checkoutJsonText }}</pre>
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
      container: '용기 선택',
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

// 담은 맛을 {flavorId, flavorName, count} 형태로 집계 (대용량은 같은 맛 중복 선택 가능)
const selectedFlavorSummary = computed(() => {
  const counts = new Map()
  for (const flavorId of selectedFlavorIds.value) {
    counts.set(flavorId, (counts.get(flavorId) ?? 0) + 1)
  }
  return Array.from(counts.entries()).map(([flavorId, count]) => ({
    flavorId,
    count,
    flavorName: flavors.value.find((f) => f.flavorId === flavorId)?.flavorName ?? ''
  }))
})

function flavorSelectedCount(flavorId) {
  return selectedFlavorIds.value.filter((id) => id === flavorId).length
}

function canPickMoreFlavor() {
  // 사이즈 상관없이 총 개수(selectableFlavorCount)만 다 안 찼으면 같은 맛도 계속 담을 수 있다
  return selectedFlavorIds.value.length < selectedProduct.value.selectableFlavorCount
}

// 등급별 적립률: Friend 3% / Family 5% / VIP 8%
const EARN_RATE = { FRIEND: 0.03, FAMILY: 0.05, VIP: 0.08 }

// 조회했는데 기존 회원이 아니면, 결제 시 FRIEND 등급으로 자동 가입되어 적립 대상이 된다 (OrderService.checkout 참고)
const isNewMember = computed(() => customerLookupDone.value && !customer.value && mobileNumberInput.value.trim() !== '')

const estimatedEarnedPoints = computed(() => {
  // 포인트를 사용한 결제건은 적립되지 않는다 (백엔드 PaymentService와 동일 규칙)
  if (cart.usedPoints > 0) return 0
  const grade = customer.value?.grade ?? (isNewMember.value ? 'FRIEND' : null)
  if (!grade) return 0
  return Math.floor(cart.totalAmount * (EARN_RATE[grade] ?? 0))
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
}

function selectProduct(product) {
  editingItemId.value = null
  resetFlavorStepState(product)

  if (product.containerPolicy === 'CUP_OR_CONE') {
    // 컵/콘 둘 다 가능한 상품만 용기 선택 화면을 보여주고, 컵만 되는 상품(파인트/패밀리/하프갤런 등)은 건너뜀
    step.value = 'container'
    return
  }
  proceedPastContainer()
}

function proceedPastContainer() {
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
  // 같은 맛을 여러 번(스쿱 여러 개) 선택할 수 있다 - 총 개수가 찰 때까지 계속 추가, 제거는 하단 요약바에서
  if (selectedFlavorIds.value.length < selectedProduct.value.selectableFlavorCount) {
    selectedFlavorIds.value.push(flavorId)
  }
}

function removeOneFlavor(flavorId) {
  const index = selectedFlavorIds.value.indexOf(flavorId)
  if (index !== -1) selectedFlavorIds.value.splice(index, 1)
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

// 결제 대행사(PG) 쪽에 그대로 넘길 수 있는 결제 요청 JSON - 실제 결제 연동 전까지는 화면에 그대로 노출해서 확인용으로 쓴다
const checkoutPayload = ref(null)
const checkoutJsonText = computed(() => (checkoutPayload.value ? JSON.stringify(checkoutPayload.value, null, 2) : ''))

async function startPayment() {
  checkoutError.value = ''
  checkoutInProgress.value = true
  try {
    checkoutPayload.value = {
      branchId: BRANCH_ID,
      orderType: cart.orderType,
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
    }
    const { data } = await http.post('/orders/checkout', checkoutPayload.value)
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

// QR 팝업을 닫고 결제 버튼 화면으로 되돌아간다 (결제가 끝난 건 아니므로 폴링만 멈춘다)
function closeQrModal() {
  stopPolling()
  qrInfo.value = null
  qrDataUrl.value = null
  paymentStatus.value = null
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

.flavor-summary-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fafafa;
  border-top: 1px solid #ddd;
  padding: 0.75rem 1.5rem;
}

.flavor-summary-bar ul {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.flavor-summary-bar li {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
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

.modal {
  position: relative;
}

.modal-close {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}

.payment-json {
  max-width: 320px;
  max-height: 200px;
  overflow: auto;
  background: #f4f4f4;
  padding: 0.75rem;
  text-align: left;
  font-size: 0.8rem;
=======
    <!-- CU-014: 홈버튼 - 클릭 시 초기 광고 화면으로 복귀 -->
    <button type="button" class="home-button" @click="goHome">🏠 처음으로</button>
    <p>단계: {{ orderFlow.stepLabel }}</p>

    <OrderTypeStep v-if="orderFlow.step === 'orderType'" />
    <ProductStep v-else-if="orderFlow.step === 'product'" />
    <ContainerStep v-else-if="orderFlow.step === 'container'" />
    <FlavorStep v-else-if="orderFlow.step === 'flavor'" />
    <CartStep v-else-if="orderFlow.step === 'cart'" />
    <CustomerPaymentStep v-else-if="orderFlow.step === 'customer'" />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderFlowStore } from '../../stores/orderFlow'
import { useCartStore } from '../../stores/cart'
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'

const router = useRouter()
const orderFlow = useOrderFlowStore()
const cart = useCartStore()

onMounted(() => {
  orderFlow.init()
})

onUnmounted(() => {
  orderFlow.stopPolling()
})

// CU-014: 진행 중인 주문이 있으면 확인 후 초기 화면으로 복귀
function goHome() {
  if (cart.items.length > 0 && !confirm('진행 중인 주문을 취소하고 처음 화면으로 돌아가시겠습니까?')) return
  orderFlow.stopPolling()
  cart.clear()
  router.push('/')
}
</script>

<style scoped>
.home-button {
  position: fixed;
  top: 1rem;
  left: 1rem;
  z-index: 10;
>>>>>>> branch 'develop' of https://github.com/warning1210/arrangement.git
}
</style>
