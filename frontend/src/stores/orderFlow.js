import { defineStore } from 'pinia'
import QRCode from 'qrcode'
import http from '../api/http'
import { fetchCategories, fetchProducts, fetchFlavors } from '../services/menuService'
import { useCartStore } from './cart'
import router from '../router'
import { getKioskBranchId } from '../utils/kioskSession'

// 등급별 적립률: Friend 3% / Family 5% / VIP 8%
const EARN_RATE = { FRIEND: 0.03, FAMILY: 0.05, VIP: 0.08 }

const MONTHLY_FLAVOR_NAMES = ['쵸파의 코튼캔디 크런치', '우디의 후르츠 어드벤처']

function isMonthlyFlavor(flavor) {
  return MONTHLY_FLAVOR_NAMES.includes(flavor?.flavorName)
}

const STEP_LABELS = {
  orderType: '매장/포장 선택',
  product: '상품 선택',
  container: '용기 선택',
  flavor: '맛 선택',
  cart: '장바구니 확인',
  customer: '포인트/결제',
  receipt: '주문 완료'
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
    monthlyFlavorUpgrade: false,

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
    pollTimer: null,

    // 결제 완료 후 영수증 화면에서 쓰는 값들
    receipt: null,       // GET /orders/{id}/receipt 로 받아온 영수증 데이터
    printing: false,     // 프린터 출력 요청 중인지
    printMessage: '',    // 출력 결과 안내 문구

    confirmDialog: null,  // { message, resolve } - 앱 디자인 확인 팝업 (ConfirmModal)이 떠 있을 때만 값이 있음
    subscribedToCart: false
  }),

  getters: {
    stepLabel: (state) => STEP_LABELS[state.step],

    // 사이즈(레귤러/대용량) 구분 없이, 선택된 카테고리의 상품을 가격 낮은 순으로 전부 한 화면에 보여준다.
    visibleProducts: (state) =>
      state.products
        .filter((p) => p.categoryId === state.selectedCategory?.categoryId)
        .sort((a, b) => a.basePrice - b.basePrice),

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
        const correctCount = state.selectedFlavorIds.length === state.selectedProduct.selectableFlavorCount
        const includesMonthlyFlavor = state.selectedFlavorIds.some((flavorId) =>
          isMonthlyFlavor(state.flavors.find((flavor) => flavor.flavorId === flavorId))
        )
        return correctCount && (!state.monthlyFlavorUpgrade || includesMonthlyFlavor)
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

      if (!this.subscribedToCart) {
        cart.$subscribe((mutation, state) => {
          if (this.qrInfo) {
            console.log('[장바구니 변경 감지] 생성된 결제 QR을 초기화합니다.')
            this.closeQrModal()
          }
        })
        this.subscribedToCart = true
      }

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
      this.monthlyFlavorUpgrade = false
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
      this.receipt = null
      this.printing = false
      this.printMessage = ''

      try {
        const [categoriesData, productsData, flavorsData] = await Promise.all([
          fetchCategories(),
          fetchProducts(),
          fetchFlavors(getKioskBranchId())
        ])
        this.categories = categoriesData
        this.products = productsData
        // 할인이 붙은 맛이 맨 앞에 오도록 백엔드가 이미 정렬해서 내려준다 (지점이 event_branch_flavor로 고른 맛)
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

    isMonthlyFlavorId(flavorId) {
      return isMonthlyFlavor(this.flavors.find((flavor) => flavor.flavorId === flavorId))
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
      this.monthlyFlavorUpgrade = false
    },

    // 컵/콘 선택(용기 둘 다 가능한 상품) 또는 테이크아웃 대용량 상품(숟가락/드라이아이스)은
    // 맛 선택보다 먼저 '상품정보' 화면을 거쳐야 한다 - 신규 선택/기존 항목 수정 모두 동일한 순서를 따른다
    needsContainerStep(product) {
      if (product.containerPolicy === 'CUP_OR_CONE') return true
      const isHandpackProduct = ['파인트', '쿼터', '패밀리', '하프갤런'].includes(product.productName)
      return isHandpackProduct && useCartStore().orderType === 'TAKEOUT'
    },

    selectProduct(product) {
      this.editingItemId = null
      this.resetFlavorStepState(product)

      if (this.needsContainerStep(product)) {
        this.step = 'container'
        return
      }
      this.proceedPastContainer()
    },

    async offerMonthlyFlavorUpgrade() {
      if (this.selectedProduct?.productName !== '싱글레귤러' || this.monthlyFlavorUpgrade) return false
      const accepted = await this.askConfirm(
        '이달의 맛 선택 시 500원 추가하면 더블주니어로 사이즈업!\n더블주니어로 사이즈업 하시겠어요?',
        { cancelLabel: '아니요', confirmLabel: '네' }
      )
      // 아니요를 선택해도 이달의 맛은 기존 싱글레귤러로 정상 선택한다.
      if (!accepted) return true
      const doubleJunior = this.products.find((product) => product.productName === '더블주니어')
      if (!doubleJunior) return false
      this.selectedProduct = { ...doubleJunior, basePrice: this.selectedProduct.basePrice + 500 }
      this.monthlyFlavorUpgrade = true
      return true
    },

    proceedPastContainer() {
      if (!this.selectedProduct.requiresFlavorSelection && !this.selectedProduct.isLarge) {
        // 추가 옵션이 전혀 없는 상품은 바로 장바구니에 담고 확인 단계로 이동한다.
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
      this.monthlyFlavorUpgrade = Boolean(item.monthlyFlavorUpgrade)
      this.step = this.needsContainerStep(product) ? 'container' : 'flavor'
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
        imageUrl: this.selectedProduct.imageUrl,
        unitPrice: this.selectedProduct.basePrice + (this.containerType === 'WAFFLE_CONE' ? 500 : 0),
        monthlyFlavorUpgrade: this.monthlyFlavorUpgrade,
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

    async removeFromCart(id) {
      const cart = useCartStore()
      // CU-007-1: 삭제 전 재확인 - 브라우저 기본 confirm() 대신 앱 디자인의 팝업으로 확인받는다
      if (await this.askConfirm('이 메뉴를 삭제하시겠습니까?')) {
        cart.removeItem(id)
      }
    },

    // 화면 디자인에 맞는 확인 팝업(ConfirmModal, OrderView.vue에 마운트됨)을 띄우고 결과를 기다린다
    askConfirm(message, options = {}) {
      return new Promise((resolve) => {
        this.confirmDialog = {
          message,
          resolve,
          noticeOnly: false,
          cancelLabel: options.cancelLabel ?? '취소',
          confirmLabel: options.confirmLabel ?? '확인'
        }
      })
    },

    showNotice(message) {
      return new Promise((resolve) => {
        this.confirmDialog = { message, resolve, noticeOnly: true }
      })
    },

    resolveConfirm(result) {
      this.confirmDialog?.resolve(result)
      this.confirmDialog = null
    },

    // 결제 완료된 주문의 영수증 데이터를 백엔드에서 불러온다 (화면 표시용)
    async loadReceipt() {
      const { data } = await http.get(`/orders/${this.orderId}/receipt`)
      this.receipt = data
    },

    // 강사님 프린터 서비스로 실제 영수증 출력을 요청한다.
    // 백엔드가 대신 프린터 서버(:8888)로 요청을 보내고, 성공/오프라인 여부만 돌려준다.
    async printReceipt() {
      this.printing = true
      this.printMessage = ''
      try {
        const { data } = await http.post(`/orders/${this.orderId}/print`)
        this.printMessage = data.printed
          ? '영수증이 출력되었습니다.'
          : '프린터에 연결하지 못했습니다. 화면의 영수증을 확인해주세요.'
      } catch (e) {
        this.printMessage = '영수증 출력 요청 중 오류가 발생했습니다.'
      } finally {
        this.printing = false
      }
    },

    // 영수증 확인 후 "완료" - 다음 손님을 위해 장바구니를 비우고 광고 화면으로 돌아간다
    finishOrder() {
      const cart = useCartStore()
      this.stopPolling()
      cart.clear()
      router.push('/')
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

    // CU-014: 화면 우측/좌측 상단 X버튼 공통 동작.
    // 장바구니 삭제(+광고 화면 복귀)는 메인화면(상품 고르는 화면)에서 눌렀을 때만 일어난다.
    // 그 외 화면(용기/맛선택, 장바구니, 결제 등)에서는 장바구니를 그대로 둔 채 메인화면으로만 되돌린다.
    async goHome() {
      const cart = useCartStore()
      this.stopPolling()
      if (this.step === 'orderType') {
        router.push('/')
        return
      }
      if (this.step !== 'product') {
        // 결제 QR을 띄운 채로 나가면 폴링이 멈춰서 결제가 완료돼도 감지를 못 하고 장바구니가 영영 안 비워진다 -
        // 진행 중이던 결제 시도 자체를 폐기해서, 나중에 '다음단계(결제하기)'를 다시 누르면 새 QR/주문으로 시작하게 한다.
        this.qrInfo = null
        this.qrDataUrl = null
        this.orderId = null
        this.checkoutPayload = null
        this.paymentStatus = null
        this.step = 'product'
        return
      }
      if (cart.items.length > 0 && !(await this.askConfirm('진행 중인 주문을 취소하고 처음 화면으로 돌아가시겠습니까?'))) return
      cart.clear()
      router.push('/')
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
            this.qrInfo = null // QR 팝업을 닫는다
            cart.clear() // 결제 성공 즉시 장바구니(쿠키) 초기화
            // 영수증 데이터를 불러와 영수증 화면으로 이동하고, 프린터로도 자동 출력을 시도한다.
            // (프린터가 없어도 화면 영수증은 그대로 보이고, 흐름은 계속 진행된다)
            await this.loadReceipt()
            this.step = 'receipt'
            this.printReceipt()
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
          branchId: getKioskBranchId(), // 이 키오스크가 부팅 시 로그인해서 기억해둔 소속 지점
          orderType: cart.orderType,
          customerMobileNumber: cart.customerMobileNumber,
          usedPoints: cart.usedPoints,
          couponCode: cart.couponCode || null,
          language: 'ko',
          // 백엔드 요청에는 quantity가 없으므로 장바구니 수량만큼 주문 항목을 펼친다.
          items: cart.items.flatMap((item) =>
            Array.from({ length: item.quantity }, () => ({
              productId: item.productId,
              containerType: item.containerType,
              spoonCount: item.spoonCount,
              dryIceMinutes: item.dryIceMinutes,
              flavorIds: item.flavors.map((f) => f.flavorId),
              monthlyFlavorUpgrade: Boolean(item.monthlyFlavorUpgrade)
            }))
          )
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
