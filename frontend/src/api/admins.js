import http from './http'

export function fetchAdmins() {
  return http.get('/admins').then((res) => res.data.data)
}
