import { defineStore } from 'pinia'

const STORAGE_KEY = 'kiosk-dev-actor'

/**
 * 임시 스캐폴딩: 실제 로그인(인증)이 구현되기 전까지, 현재 화면을 사용 중인 관리자를
 * 흉내내기 위한 스토어. 여기 저장된 adminId가 API 요청 헤더(X-Admin-Id)에 자동으로 실린다.
 * 실제 로그인이 도입되면 이 스토어는 로그인 응답으로 채워지도록 교체하면 된다.
 */
export const useDevActorStore = defineStore('devActor', {
  state: () => ({
    admin: loadFromStorage()
  }),
  getters: {
    isSelected: (state) => !!state.admin,
    adminId: (state) => state.admin?.adminId ?? null,
    role: (state) => state.admin?.role ?? null
  },
  actions: {
    select(admin) {
      this.admin = admin
      localStorage.setItem(STORAGE_KEY, JSON.stringify(admin))
    },
    clear() {
      this.admin = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}
