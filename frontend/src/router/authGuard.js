// 로그인 없이 접근 가능해야 하는 지점 경로 (로그인/가입/개설 신청서).
const PUBLIC_BRANCH_PATHS = ['/branch/login', '/branch/join', '/branch/application']

// 순수 함수로 분리 - .vue 컴포넌트 임포트가 없어서 vite 없이도 바로 테스트 가능하다.
export function resolveGuard(path, fullPath, readSession) {
  if (path.startsWith('/admin/') && path !== '/admin/login') {
    const session = readSession('hq-session')
    if (session?.token) return true
    return { path: '/branch/login', query: { redirect: fullPath } }
  }
  if (path.startsWith('/branch/') && !PUBLIC_BRANCH_PATHS.includes(path)) {
    const session = readSession('branch-session')
    if (session?.token) return true
    return { path: '/branch/login', query: { redirect: fullPath } }
  }
  return true
}
