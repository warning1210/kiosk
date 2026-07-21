import { defineStore } from 'pinia'
import { getCookie, setCookie, removeCookie } from '../utils/cookie'

const CART_COOKIE_NAME = 'kiosk_cart'

// crypto.randomUUID()는 HTTPS/localhost 같은 "보안 컨텍스트"에서만 존재해서,
// LAN IP(http://192.168.x.x 등)로 접속하면 없다. crypto.getRandomValues()는 보안 컨텍스트가 아니어도 쓸 수 있으므로 그걸로 대체 생성한다.
function generateId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

const EMPTY_STATE = () => ({
  orderType: null,
  customerMobileNumber: null,
  usedPoints: 0,
  couponCode: null,
  items: []
})

// 쿠키엔 JSON 문자열 하나로 저장한다. 매장/포장, 고객번호/사용포인트까지
// 같이 실어서, 단계를 오가거나 새로고침해도 지금까지 진행 상태가 유지되게 한다.
function loadFromCookie() {
  const raw = getCookie(CART_COOKIE_NAME)
  if (!raw) return EMPTY_STATE()
  try {
    const parsed = JSON.parse(raw)
    return {
      ...EMPTY_STATE(),
      ...parsed,
      items: Array.isArray(parsed.items) ? parsed.items : []
    }
  } catch {
    return EMPTY_STATE()
  }
}

function persist(state) {
  if (!state.orderType && state.items.length === 0) {
    removeCookie(CART_COOKIE_NAME)
  } else {
    setCookie(
      CART_COOKIE_NAME,
      JSON.stringify({
        orderType: state.orderType,
        customerMobileNumber: state.customerMobileNumber,
        usedPoints: state.usedPoints,
        couponCode: state.couponCode,
        items: state.items
      })
    )
  }
}

function itemSignature(item) {
  const flavorIds = (item.flavors ?? [])
    .map((flavor) => flavor.flavorId)
    .sort((a, b) => Number(a) - Number(b))
  return JSON.stringify({
    productId: item.productId,
    unitPrice: item.unitPrice,
    containerType: item.containerType ?? 'NONE',
    spoonCount: item.spoonCount ?? 0,
    dryIceMinutes: item.dryIceMinutes ?? null,
    monthlyFlavorUpgrade: Boolean(item.monthlyFlavorUpgrade),
    flavorIds
  })
}

export const useCartStore = defineStore('cart', {
  state: () => loadFromCookie(),

  getters: {
    totalCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    amountBeforeDiscount: (state) => state.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0),
    totalAmount() {
      return Math.max(0, this.amountBeforeDiscount - this.usedPoints)
    }
  },

  actions: {
    setOrderType(orderType) {
      this.orderType = orderType
      persist(this)
    },

    setCustomer(mobileNumber) {
      this.customerMobileNumber = mobileNumber
      persist(this)
    },

    setUsedPoints(points) {
      this.usedPoints = points
      persist(this)
    },

    setCouponCode(code) {
      this.couponCode = code
      persist(this)
    },

    // item: { productId, productName, unitPrice, quantity, containerType, spoonCount, dryIceMinutes, flavors }
    addItem(item) {
      const newItem = {
        id: generateId(),
        quantity: 1,
        containerType: 'NONE',
        spoonCount: 0,
        dryIceMinutes: null,
        flavors: [],
        ...item
      }
      const existingItem = this.items.find((cartItem) => itemSignature(cartItem) === itemSignature(newItem))
      if (existingItem) {
        existingItem.quantity += newItem.quantity
      } else {
        this.items.push(newItem)
      }
      persist(this)
      console.log('[주문 내역] 상품 담김:', newItem)
      console.log('[주문 내역] 전체 장바구니:', this.items)
    },

    updateItem(id, changes) {
      const index = this.items.findIndex((item) => item.id === id)
      if (index === -1) return
      const updatedItem = { ...this.items[index], ...changes }
      const duplicateIndex = this.items.findIndex((item, itemIndex) =>
        itemIndex !== index && itemSignature(item) === itemSignature(updatedItem)
      )
      if (duplicateIndex !== -1) {
        this.items[duplicateIndex].quantity += updatedItem.quantity
        this.items.splice(index, 1)
      } else {
        this.items[index] = updatedItem
      }
      persist(this)
      console.log('[주문 내역] 상품 수정됨:', this.items[index])
      console.log('[주문 내역] 전체 장바구니:', this.items)
    },

    removeItem(id) {
      this.items = this.items.filter((item) => item.id !== id)
      persist(this)
    },

    adjustQuantity(id, delta) {
      const item = this.items.find((cartItem) => cartItem.id === id)
      if (!item) return
      item.quantity = Math.max(1, item.quantity + delta)
      persist(this)
    },

    clear() {
      Object.assign(this, EMPTY_STATE())
      persist(this)
    }
  }
})
