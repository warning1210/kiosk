import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 본점 토큰은 httpOnly 쿠키에 들어있어서 JS가 붙일 일이 없다 - 같은 출처라 브라우저가 요청마다
// 자동으로 실어 보내고, 서버는 AuthCookieFilter가 그 쿠키를 Authorization 헤더로 바꿔준다.
// (요청 인터셉터를 아예 두지 않는 이유: 예전에 여기서 하던 일이 그것 하나뿐이었다.)

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
