// 로그인 없이 접근 가능해야 하는 지점 경로 (로그인/가입/개설 신청서).
const PUBLIC_BRANCH_PATHS = ['/branch/login', '/branch/join', '/branch/application']

// 순수 함수로 분리 - .vue 컴포넌트 임포트가 없어서 vite 없이도 바로 테스트 가능하다.
export function resolveGuard(path, fullPath, readSession) {
  // 세션 객체의 존재만 본다. 예전에는 token 유무로 판단했는데 토큰이 httpOnly 쿠키로 옮겨가면서
  // JS에서 보이지 않게 됐다 - 어차피 이 가드는 화면 전환용 힌트일 뿐이고, 실제 인가는 매 요청마다
  // 서버가 한다(위조된 세션 객체를 넣어도 API가 전부 401을 준다).
  if (path.startsWith('/admin/') && path !== '/admin/login') {
    if (readSession('hq-session')?.adminId) return true
    return { path: '/branch/login', query: { redirect: fullPath } }
  }
  if (path.startsWith('/branch/') && !PUBLIC_BRANCH_PATHS.includes(path)) {
    if (readSession('branch-session')?.branchId) return true
    return { path: '/branch/login', query: { redirect: fullPath } }
  }
  return true
}
