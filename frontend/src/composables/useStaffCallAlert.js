import { onMounted, onUnmounted, ref } from 'vue'
import http from '../api/branch'

const POLL_INTERVAL_MS = 5_000

// 키오스크에서 온 "직원 호출"을 지점 관리자 화면 어디서든(사이드바) 보여주기 위한 공용 로직.
// 호출은 급한 신호라 다른 지점 폴링(2~3초)보다 빠듯한 5초 주기로 확인한다.
export function useStaffCallAlert() {
  const called = ref(false)
  let timer

  async function refresh() {
    try {
      const status = (await http.get('/staff-call')).data
      called.value = !!status.called
    } catch (e) {
      console.error(e)
    }
  }

  async function acknowledge() {
    if (!called.value) return
    try {
      await http.delete('/staff-call')
      called.value = false
    } catch (e) {
      console.error(e)
    }
  }

  onMounted(() => {
    refresh()
    timer = window.setInterval(refresh, POLL_INTERVAL_MS)
  })

  onUnmounted(() => window.clearInterval(timer))

  return { called, acknowledge }
}
