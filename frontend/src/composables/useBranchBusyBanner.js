import { onMounted, onUnmounted, ref } from 'vue'
import { fetchBranchStatus } from '../services/branchStatusService'
import { getKioskBranchId } from '../utils/kioskSession'

const POLL_INTERVAL_MS = 15_000

// 지점 관리자가 "바빠요"로 표시한 대기시간을 키오스크 화면(광고/주문)에서 함께 폴링해서 보여주기 위한 공용 로직.
// 이 프로젝트엔 WebSocket이 없고 다른 화면들도 모두 setInterval 폴링을 쓰므로 그 패턴을 따른다.
export function useBranchBusyBanner() {
  const isBusy = ref(false)
  const estimatedWaitMinutes = ref(null)
  let timer

  async function refresh() {
    const branchId = getKioskBranchId()
    if (!branchId) return
    try {
      const status = await fetchBranchStatus(branchId)
      isBusy.value = !!status.isBusy
      estimatedWaitMinutes.value = status.estimatedWaitMinutes
    } catch (e) {
      console.error(e)
    }
  }

  onMounted(() => {
    refresh()
    timer = window.setInterval(refresh, POLL_INTERVAL_MS)
  })

  onUnmounted(() => window.clearInterval(timer))

  return { isBusy, estimatedWaitMinutes }
}
