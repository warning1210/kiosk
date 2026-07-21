import { onUnmounted, ref } from 'vue'
import { callStaff as requestStaffCall } from '../services/staffCallService'
import { getKioskBranchId } from '../utils/kioskSession'

const COOLDOWN_MS = 30_000

// 키오스크 주문화면의 종 버튼 - 누르면 지점에 호출을 보내고, 스팸 클릭 방지를 위해 잠시 재클릭을 막는다.
export function useStaffCall() {
  const calling = ref(false)
  const justCalled = ref(false)
  let cooldownTimer

  async function callStaff() {
    if (calling.value || justCalled.value) return
    const branchId = getKioskBranchId()
    if (!branchId) return
    calling.value = true
    try {
      await requestStaffCall(branchId)
      justCalled.value = true
      cooldownTimer = window.setTimeout(() => { justCalled.value = false }, COOLDOWN_MS)
    } catch (e) {
      console.error(e)
    } finally {
      calling.value = false
    }
  }

  onUnmounted(() => window.clearTimeout(cooldownTimer))

  return { calling, justCalled, callStaff }
}
