import http from './http'

export function fetchBranchStockRequests({ status, page = 0, size = 20 } = {}) {
  return http
    .get('/branch/stock-requests', { params: { status, page, size } })
    .then((res) => res.data.data)
}

export function createStockRequest(payload) {
  return http.post('/branch/stock-requests', payload).then((res) => res.data.data)
}

export function cancelStockRequest(id) {
  return http.patch(`/branch/stock-requests/${id}/cancel`).then((res) => res.data.data)
}

export function confirmReceipt(id) {
  return http.patch(`/branch/stock-requests/${id}/confirm-receipt`).then((res) => res.data.data)
}
