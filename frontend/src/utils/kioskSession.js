const KIOSK_SESSION_KEY = 'kiosk-session'

// 이 키오스크 기기가 처음 부팅됐을 때 지점 관리자 계정으로 한 번 로그인해서 "어느 지점 소속인지"를
// 기억해두는 값. { branchId, branchName }만 저장한다 - 관리자 토큰/자격증명은 남기지 않는다
// (손님이 만지는 화면에 관리자 세션이 남아있으면 안 되므로).
export function getKioskSession() {
  try {
    return JSON.parse(localStorage.getItem(KIOSK_SESSION_KEY))
  } catch {
    return null
  }
}

export function setKioskSession(session) {
  localStorage.setItem(KIOSK_SESSION_KEY, JSON.stringify(session))
}

export function getKioskBranchId() {
  return getKioskSession()?.branchId ?? null
}
