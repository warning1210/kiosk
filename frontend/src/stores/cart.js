import { defineStore } from 'pinia'
import { getCookie, setCookie, removeCookie } from '../utils/cookie'

const CART_COOKIE_NAME = 'kiosk_cart'

// 쿠키엔 { orderType, items } 형태의 JSON 문자열 하나로 저장한다.
// orderType까지 같이 실어서, 단계(매장/포장 -> 상품 -> 맛 -> 장바구니)를
// 오가거나 새로고침해도 지금까지 진행 상태가 유지되게 한다.
function loadFromCookie() {
  const raw = getCookie(CART_COOKIE_NAME)
  if (!raw) return { orderType: null, items: [] }
  try {
    const parsed = JSON.parse(raw)
    return {
      orderType: parsed.orderType ?? null,
      items: Array.isArray(parsed.items) ? parsed.items : []
    }
  } catch {
    return { orderType: null, items: [] }
  }
}

function persist(state) {
  if (!state.orderType && state.items.length === 0) {
    removeCookie(CART_COOKIE_NAME)
  } else {
    setCookie(CART_COOKIE_NAME, JSON.stringify({ orderType: state.orderType, items: state.items }))
  }
}

export const useCartStore = defineStore('cart', {
  state: () => loadFromCookie(),

  getters: {
    totalCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: (state) => state.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
  },

  actions: {
    setOrderType(orderType) {
      this.orderType = orderType
      persist(this)
    },

    // item: { productId, productName, unitPrice, quantity, containerType, spoonCount, dryIceMinutes, requestNote, flavors }
    addItem(item) {
      this.items.push({
        id: crypto.randomUUID(),
        quantity: 1,
        containerType: 'NONE',
        spoonCount: 0,
        dryIceMinutes: null,
        requestNote: '',
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
      this.orderType = null
      this.items = []
      persist(this)
    }
  }
})
