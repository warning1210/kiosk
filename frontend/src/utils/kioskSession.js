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

// 숨겨진 키오스크 로그아웃에서 기기에 저장된 소속 지점만 해제한다.
// 관리자 인증 토큰은 애초에 키오스크 세션에 저장하지 않으므로 이 값만 지우면 최초 등록 화면으로 돌아간다.
export function clearKioskSession() {
  localStorage.removeItem(KIOSK_SESSION_KEY)
}

export function getKioskBranchId() {
  return getKioskSession()?.branchId ?? null
}
