import { defineStore } from 'pinia'
import QRCode from 'qrcode'
import http from '../api/http'
import { fetchCategories, fetchProducts, fetchFlavors } from '../services/menuService'
import { useCartStore } from './cart'
import router from '../router'

// 등급별 적립률: Friend 3% / Family 5% / VIP 8%
const EARN_RATE = { FRIEND: 0.03, FAMILY: 0.05, VIP: 0.08 }

const STEP_LABELS = {
  orderType: '매장/포장 선택',
  product: '상품 선택',
  container: '용기 선택',
  flavor: '맛 선택',
  cart: '장바구니 확인',
  customer: '포인트/결제'
}

const PAYMENT_STATUS_LABELS = {
  QR_CREATED: '스캔 대기 중',
  PAID: '결제 완료',
  FAILED: '결제 실패',
  EXPIRED: '유효시간 만료',
  CANCELLED: '취소됨'
}

export const useOrderFlowStore = defineStore('orderFlow', {
  state: () => ({
    step: 'orderType',

    categories: [],
    products: [],
    flavors: [],
    loading: true,
    loadError: false,

    selectedCategory: null,
    selectedProduct: null,
    editingItemId: null,
    selectedFlavorIds: [],
    containerType: 'NONE',
    spoonCount: 0,
    dryIceMinutes: null,

    mobileNumberInput: '',
    customer: null,
    customerLookupDone: false,

    checkoutInProgress: false,
    checkoutError: '',
    orderId: null,
    qrInfo: null,
    qrDataUrl: null,
    paymentStatus: null,
    checkoutPayload: null,
    pollTimer: null
  }),

  getters: {
    stepLabel: (state) => STEP_LABELS[state.step],

    // 사이즈(레귤러/대용량) 구분 없이, 선택된 카테고리의 상품을 전부 한 화면에 보여준다.
    visibleProducts: (state) => state.products.filter((p) => p.categoryId === state.selectedCategory?.categoryId),

    // 담은 맛을 {flavorId, flavorName, count} 형태로 집계 (대용량은 같은 맛 중복 선택 가능)
    selectedFlavorSummary: (state) => {
      const counts = new Map()
      for (const flavorId of state.selectedFlavorIds) {
        counts.set(flavorId, (counts.get(flavorId) ?? 0) + 1)
      }
      return Array.from(counts.entries()).map(([flavorId, count]) => ({
        flavorId,
        count,
        flavorName: state.flavors.find((f) => f.flavorId === flavorId)?.flavorName ?? ''
      }))
    },

    canConfirmFlavor: (state) => {
      if (!state.selectedProduct) return false
      if (state.selectedProduct.requiresFlavorSelection) {
        return state.selectedFlavorIds.length === state.selectedProduct.selectableFlavorCount
      }
      return true
    },

    // 조회했는데 기존 회원이 아니면, 결제 시 FRIEND 등급으로 자동 가입되어 적립 대상이 된다 (백엔드 OrderService.checkout 참고)
    isNewMember: (state) => state.customerLookupDone && !state.customer && state.mobileNumberInput.trim() !== '',

    estimatedEarnedPoints(state) {
      const cart = useCartStore()
      // 포인트를 사용한 결제건은 적립되지 않는다 (백엔드 PaymentService와 동일 규칙)
      if (cart.usedPoints > 0) return 0
      const grade = state.customer?.grade ?? (this.isNewMember ? 'FRIEND' : null)
      if (!grade) return 0
      return Math.floor(cart.totalAmount * (EARN_RATE[grade] ?? 0))
    },

    paymentStatusLabel: (state) => PAYMENT_STATUS_LABELS[state.paymentStatus] ?? state.paymentStatus,

    // 결제 대행사(PG) 쪽에 그대로 넘길 수 있는 결제 요청 JSON - 실제 결제 연동 전까지는 화면에 그대로 노출해서 확인용으로 쓴다
    checkoutJsonText: (state) => (state.checkoutPayload ? JSON.stringify(state.checkoutPayload, null, 2) : '')
  },

  actions: {
    // 매 진입(마운트)마다 이전 고객의 진행 상태가 이어지면 안 되므로 전부 초기화하고 메뉴를 새로 불러온다
    async init() {
      const cart = useCartStore()
      this.stopPolling()

      this.step = cart.orderType ? 'product' : 'orderType'
      this.categories = []
      this.products = []
      this.flavors = []
      this.loading = true
      this.loadError = false
      this.selectedCategory = null
      this.selectedProduct = null
      this.editingItemId = null
      this.selectedFlavorIds = []
      this.containerType = 'NONE'
      this.spoonCount = 0
      this.dryIceMinutes = null
      this.mobileNumberInput = ''
      this.customer = null
      this.customerLookupDone = false
      this.checkoutInProgress = false
      this.checkoutError = ''
      this.orderId = null
      this.qrInfo = null
      this.qrDataUrl = null
      this.paymentStatus = null
      this.checkoutPayload = null

      try {
        const [categoriesData, productsData, flavorsData] = await Promise.all([
          fetchCategories(),
          fetchProducts(),
          fetchFlavors()
        ])
        this.categories = categoriesData
        this.products = productsData
        this.flavors = flavorsData
        this.selectedCategory = categoriesData[0] ?? null
      } catch (e) {
        this.loadError = true
      } finally {
        this.loading = false
      }
    },

    flavorSelectedCount(flavorId) {
      return this.selectedFlavorIds.filter((id) => id === flavorId).length
    },

    canPickMoreFlavor() {
      // 사이즈 상관없이 총 개수(selectableFlavorCount)만 다 안 찼으면 같은 맛도 계속 담을 수 있다
      return this.selectedFlavorIds.length < this.selectedProduct.selectableFlavorCount
    },

    selectOrderType(orderType) {
      const cart = useCartStore()
      cart.setOrderType(orderType)
      this.step = 'product'
    },

    resetFlavorStepState(product) {
      this.selectedProduct = product
      this.selectedFlavorIds = []
      this.containerType = product.containerPolicy === 'NONE' ? 'NONE' : 'CUP'
      this.spoonCount = 0
      this.dryIceMinutes = null
    },

    selectProduct(product) {
      this.editingItemId = null
      this.resetFlavorStepState(product)

      if (product.containerPolicy === 'CUP_OR_CONE') {
        // 컵/콘 둘 다 가능한 상품만 용기 선택 화면을 보여주고, 컵만 되는 상품(파인트/패밀리/하프갤런 등)은 건너뜀
        this.step = 'container'
        return
      }
      this.proceedPastContainer()
    },

    proceedPastContainer() {
      if (!this.selectedProduct.requiresFlavorSelection && !this.selectedProduct.isLarge) {
        // 추가 옵션이 전혀 없는 상품(예: 아메리카노)은 바로 장바구니에 담고 확인 단계로 이동
        this.addCurrentSelectionToCart()
        this.step = 'cart'
        return
      }
      this.step = 'flavor'
    },

    editItem(item) {
      const product = this.products.find((p) => p.productId === item.productId)
      if (!product) return
      this.editingItemId = item.id
      this.selectedCategory = this.categories.find((c) => c.categoryId === product.categoryId) ?? this.selectedCategory
      this.selectedProduct = product
      this.selectedFlavorIds = item.flavors.map((f) => f.flavorId)
      this.containerType = item.containerType
      this.spoonCount = item.spoonCount
      this.dryIceMinutes = item.dryIceMinutes
      this.step = 'flavor'
    },

    toggleFlavor(flavorId) {
      // 같은 맛을 여러 번(스쿱 여러 개) 선택할 수 있다 - 총 개수가 찰 때까지 계속 추가, 제거는 하단 요약바에서
      if (this.selectedFlavorIds.length < this.selectedProduct.selectableFlavorCount) {
        this.selectedFlavorIds.push(flavorId)
      }
    },

    removeOneFlavor(flavorId) {
      const index = this.selectedFlavorIds.indexOf(flavorId)
      if (index !== -1) this.selectedFlavorIds.splice(index, 1)
    },

    addCurrentSelectionToCart() {
      const cart = useCartStore()
      const selectedFlavors = this.selectedFlavorIds.map((flavorId, index) => {
        const flavor = this.flavors.find((f) => f.flavorId === flavorId)
        return { flavorId, flavorName: flavor?.flavorName ?? '', selectionOrder: index + 1, quantity: 1 }
      })

      const payload = {
        productId: this.selectedProduct.productId,
        productName: this.selectedProduct.productName,
        unitPrice: this.selectedProduct.basePrice,
        containerType: this.containerType,
        spoonCount: this.spoonCount,
        dryIceMinutes: this.dryIceMinutes,
        flavors: selectedFlavors
      }

      if (this.editingItemId) {
        cart.updateItem(this.editingItemId, payload)
      } else {
        cart.addItem(payload)
      }
    },

    confirmAddToCart() {
      this.addCurrentSelectionToCart()
      this.step = 'cart'
    },

    removeFromCart(id) {
      const cart = useCartStore()
      // CU-007-1: 삭제 전 재확인
      if (confirm('이 메뉴를 삭제하시겠습니까?')) {
        cart.removeItem(id)
      }
    },

    async lookupCustomer() {
      const cart = useCartStore()
      this.customerLookupDone = false
      this.customer = null
      if (!this.mobileNumberInput) return
      try {
        const { data } = await http.get(`/customers/${this.mobileNumberInput}`)
        this.customer = data
        cart.setCustomer(this.mobileNumberInput)
      } catch (e) {
        this.customer = null
      } finally {
        this.customerLookupDone = true
      }
    },

    adjustUsedPoints(delta) {
      const cart = useCartStore()
      if (!this.customer) return
      const next = Math.min(this.customer.pointBalance, cart.amountBeforeDiscount, Math.max(0, cart.usedPoints + delta))
      cart.setUsedPoints(next)
    },

    useMaxPoints() {
      const cart = useCartStore()
      if (!this.customer) return
      cart.setUsedPoints(Math.min(this.customer.pointBalance, cart.amountBeforeDiscount))
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    startPolling(qrToken) {
      const cart = useCartStore()
      this.stopPolling()
      this.pollTimer = setInterval(async () => {
        try {
          const { data } = await http.get(`/payments/${qrToken}`)
          this.paymentStatus = data.paymentStatus
          if (data.paymentStatus === 'PAID') {
            this.stopPolling()
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
    },

    async requestQr() {
      const { data } = await http.post('/payments/qr', { orderId: this.orderId })
      this.qrInfo = data
      this.paymentStatus = 'QR_CREATED'
      this.qrDataUrl = await QRCode.toDataURL(`${window.location.origin}/pay/${data.qrToken}`)
      this.startPolling(data.qrToken)
    },

    async startPayment() {
      const cart = useCartStore()
      this.checkoutError = ''
      this.checkoutInProgress = true
      try {
        this.checkoutPayload = {
          branchId: 1, // 지금은 키오스크 1대 = 지점 1곳으로 가정하고 고정값 사용 (지점/로그인 붙기 전까지 임시)
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
        const { data } = await http.post('/orders/checkout', this.checkoutPayload)
        this.orderId = data.orderId
        await this.requestQr()
      } catch (e) {
        this.checkoutError = e.response?.data?.message ?? '결제 요청에 실패했습니다.'
      } finally {
        this.checkoutInProgress = false
      }
    },

    // CU-009-1: 결제 실패(QR 만료 등) 시 QR코드를 재생성하여 다시 시도
    async regenerateQr() {
      await this.requestQr()
    },

    // QR 팝업을 닫고 결제 버튼 화면으로 되돌아간다 (결제가 끝난 건 아니므로 폴링만 멈춘다)
    closeQrModal() {
      this.stopPolling()
      this.qrInfo = null
      this.qrDataUrl = null
      this.paymentStatus = null
    }
  }
})
