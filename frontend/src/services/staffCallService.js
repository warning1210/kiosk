import http from '../api/http'

export async function callStaff(branchId) {
  return (await http.post('/staff-call', { branchId })).data
}
