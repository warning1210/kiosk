import axios from 'axios'
import { useDevActorStore } from '../stores/devActor'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 임시 스캐폴딩: 실제 로그인이 붙기 전까지 devActor 스토어의 adminId를 헤더로 실어보낸다.
http.interceptors.request.use((config) => {
  const devActor = useDevActorStore()
  if (devActor.adminId) {
    config.headers['X-Admin-Id'] = devActor.adminId
  }
  return config
})

export default http
