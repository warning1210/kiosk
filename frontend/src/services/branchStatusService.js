import http from '../api/http'

export async function fetchBranchStatus(branchId) {
  return (await http.get('/branch-status', { params: { branchId } })).data
}
