import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 본점은 Firebase 대신 자체 로그인 토큰(HqAccessService가 검증)을 쓴다.
api.interceptors.request.use((config) => {
  const session = JSON.parse(localStorage.getItem('hq-session') || 'null')
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`
  return config
})

// 본점 토큰이 없거나 만료되어 서버가 401을 반환하면 잘못 남은 세션을 제거한다.
api.interceptors.response.use(
  // 정상 응답은 수정하지 않고 호출한 화면으로 그대로 전달한다.
  (response) => response,
  // 인증 실패 응답만 공통으로 처리하고 나머지 오류는 각 화면이 처리하게 한다.
  (requestError) => {
    // 본점 API에서 401을 받으면 사용할 수 없는 토큰을 브라우저에서 삭제한다.
    if (requestError.response?.status === 401) {
      localStorage.removeItem('hq-session')
      // 로그인 화면에서 다시 401이 발생하는 경우에는 이동을 반복하지 않는다.
      if (window.location.pathname !== '/branch/login') window.location.replace('/branch/login')
    }
    // 기존 호출부의 오류 처리도 동작하도록 원래 오류를 다시 반환한다.
    return Promise.reject(requestError)
  }
)

export default api
