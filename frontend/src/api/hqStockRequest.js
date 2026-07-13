import http from './http'

export function fetchHqStockRequests({ status, branchId, keyword, from, to, page = 0, size = 20 } = {}) {
  return http
    .get('/hq/stock-requests', { params: { status, branchId, keyword, from, to, page, size } })
    .then((res) => res.data)
}

export function fetchHqStockRequestSummary() {
  return http.get('/hq/stock-requests/summary').then((res) => res.data)
}

export function approveStockRequest(id) {
  return http.patch(`/hq/stock-requests/${id}/approve`).then((res) => res.data)
}

export function rejectStockRequest(id, rejectionReason) {
  return http.patch(`/hq/stock-requests/${id}/reject`, { rejectionReason }).then((res) => res.data)
}

export function shipStockRequest(id, payload) {
  return http.patch(`/hq/stock-requests/${id}/ship`, payload).then((res) => res.data)
}
