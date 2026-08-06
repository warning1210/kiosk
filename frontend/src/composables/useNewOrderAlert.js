import { ref, onMounted, onUnmounted } from 'vue'
import http from '../api/branch'
import { useBranchEventStream } from './useBranchEventStream'

const newOrdersCount = ref(0)
let subscribers = 0
let unsubscribeOrder = null

export function useNewOrderAlert() {
  const { onOrder } = useBranchEventStream()

  async function refresh() {
    try {
      const res = await http.get('/orders')
      const today = new Date().toDateString()
      newOrdersCount.value = res.data.filter(
        o => o.status === 'PAID' && new Date(o.createdAt).toDateString() === today
      ).length
    } catch (e) {
      console.error('Failed to fetch new orders alert:', e)
    }
  }

  onMounted(() => {
    subscribers++
    if (subscribers === 1) {
      refresh()
      unsubscribeOrder = onOrder(refresh)
    }
  })

  onUnmounted(() => {
    subscribers--
    if (subscribers === 0) {
      unsubscribeOrder?.()
      unsubscribeOrder = null
    }
  })

  return { newOrdersCount, refresh }
}
