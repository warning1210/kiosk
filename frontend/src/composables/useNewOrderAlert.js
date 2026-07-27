import { ref, onMounted, onUnmounted } from 'vue'
import http from '../api/branch'

const newOrdersCount = ref(0)
let timer = null
let subscribers = 0

const POLL_INTERVAL = 3000

export function useNewOrderAlert() {
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
      timer = window.setInterval(refresh, POLL_INTERVAL)
    }
  })

  onUnmounted(() => {
    subscribers--
    if (subscribers === 0) {
      window.clearInterval(timer)
      timer = null
    }
  })

  return { newOrdersCount, refresh }
}
