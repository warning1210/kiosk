import { defineStore } from 'pinia'
import { getCookie, setCookie, removeCookie } from '../utils/cookie'

const CART_COOKIE_NAME = 'kiosk_cart'

const EMPTY_STATE = () => ({
  orderType: null,
  customerMobileNumber: null,
  usedPoints: 0,
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
        items: state.items
      })
    )
  }
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

    // item: { productId, productName, unitPrice, quantity, containerType, spoonCount, dryIceMinutes, flavors }
    addItem(item) {
      this.items.push({
        id: crypto.randomUUID(),
        quantity: 1,
        containerType: 'NONE',
        spoonCount: 0,
        dryIceMinutes: null,
        flavors: [],
        ...item
      })
      persist(this)
    },

    updateItem(id, changes) {
      const index = this.items.findIndex((item) => item.id === id)
      if (index === -1) return
      this.items[index] = { ...this.items[index], ...changes }
      persist(this)
    },

    removeItem(id) {
      this.items = this.items.filter((item) => item.id !== id)
      persist(this)
    },

    clear() {
      Object.assign(this, EMPTY_STATE())
      persist(this)
    }
  }
})
