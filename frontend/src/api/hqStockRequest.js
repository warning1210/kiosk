import http from './http'

// 본점 재고신청 화면의 URL과 정상 응답의 data 추출을 한곳에 모은다.
// 현재 화면은 오류 메시지를 위해 Axios 오류 구조를 직접 읽고, 응답 DTO의 필드도 사용한다.

/**
 * 상태, 지점, 검색어, 기간을 선택적으로 전달해 본점용 신청 목록을 조회한다.
 * page와 size는 생략할 경우 각각 0과 20을 사용한다.
 */
export function fetchHqStockRequests({ status, branchId, keyword, from, to, page = 0, size = 20 } = {}) {
  return http
    .get('/hq/stock-requests', { params: { status, branchId, keyword, from, to, page, size } })
    // 화면에서는 페이지 정보가 들어 있는 응답 본문만 사용한다.
    .then((res) => res.data)
}

/** 전체/대기/승인 후 처리/반려 신청 수를 상단 KPI용으로 조회한다. */
export function fetchHqStockRequestSummary() {
  return http.get('/hq/stock-requests/summary').then((res) => res.data)
}

/** 대기(PENDING) 신청을 승인해 배송 준비(PREPARING) 상태로 변경한다. */
export function approveStockRequest(id) {
  return http.patch(`/hq/stock-requests/${id}/approve`).then((res) => res.data)
}

/** 대기 신청을 반려하며 담당자가 입력한 반려 사유를 요청 본문에 담는다. */
export function rejectStockRequest(id, rejectionReason) {
  return http.patch(`/hq/stock-requests/${id}/reject`, { rejectionReason }).then((res) => res.data)
}

/** 배송 준비 신청에 모달에서 받은 배송 정보를 등록해 배송 중 상태로 변경한다. */
export function shipStockRequest(id, payload) {
  return http.patch(`/hq/stock-requests/${id}/ship`, payload).then((res) => res.data)
}
