import axios from 'axios'
 
// withCredentials: true 로 설정하면
// 1) 요청 시 브라우저가 갖고 있는 쿠키를 자동으로 함께 보내고
// 2) 응답의 Set-Cookie 헤더를 브라우저가 자동으로 저장합니다.
// 즉, 쿠키 값을 코드에서 직접 읽고 다시 실어보낼 필요가 없습니다.
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
})
 
export default api
 