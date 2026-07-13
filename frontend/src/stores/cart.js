import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const discountAmount = ref(0)
  const pointAction = ref(null)

  const itemCount = computed(() => items.value.length)
  const totalPrice = computed(() => items.value.reduce((sum, item) => sum + item.basePrice, 0))
  const finalPrice = computed(() => Math.max(0, totalPrice.value - discountAmount.value))

  function addItem(item) {
    items.value.push({
      cartItemId: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      ...item
    })
  }

  function removeItem(cartItemId) {
    items.value = items.value.filter((item) => item.cartItemId !== cartItemId)
  }

  function clear() {
    items.value = []
    discountAmount.value = 0
    pointAction.value = null
  }

  function applyPoints(amount) {
    discountAmount.value = Math.min(Math.max(0, Number(amount) || 0), totalPrice.value)
    pointAction.value = 'USE'
  }

  function setPointAction(action) {
    pointAction.value = action
    if (action !== 'USE') discountAmount.value = 0
  }

  return { items, itemCount, totalPrice, discountAmount, finalPrice, pointAction, addItem, removeItem, clear, applyPoints, setPointAction }
})
