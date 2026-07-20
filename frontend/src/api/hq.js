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

export default api
