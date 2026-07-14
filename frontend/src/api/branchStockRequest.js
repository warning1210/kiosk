import http from './http'

// 이 파일은 지점 재고신청 화면이 사용하는 URL과 정상 응답의 data 추출을 모은 API 계층이다.
// 현재 화면은 오류 메시지를 위해 Axios 오류 구조를 직접 읽고, 응답 DTO의 필드도 사용한다.

/**
 * 지점의 재고신청 목록을 페이지 단위로 조회한다.
 * 인자를 생략해도 첫 페이지 20건이라는 기본값으로 호출할 수 있다.
 */
export function fetchBranchStockRequests({ status, page = 0, size = 20 } = {}) {
  return http
    .get('/branch/stock-requests', { params: { status, page, size } })
    // Axios 응답 전체가 아니라 백엔드가 만든 응답 본문만 화면에 전달한다.
    .then((res) => res.data)
}

/**
 * 신청 사유, 긴급도, 상품 목록이 담긴 payload로 새 재고신청을 생성한다.
 */
export function createStockRequest(payload) {
  return http.post('/branch/stock-requests', payload).then((res) => res.data)
}

/**
 * 아직 대기 중인 신청을 취소한다.
 * 화면은 응답 본문을 쓰지 않으므로 Axios 요청 Promise를 그대로 반환해 성공·실패만 기다린다.
 */
export function cancelStockRequest(id) {
  return http.patch(`/branch/stock-requests/${id}/cancel`)
}

/**
 * 배송 중인 신청의 수령을 확정한다. 백엔드는 이 처리와 함께 지점 재고를 반영한다.
 */
export function confirmReceipt(id) {
  return http.patch(`/branch/stock-requests/${id}/confirm-receipt`).then((res) => res.data)
}
