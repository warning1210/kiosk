package com.kiosk.hq.stockrequest.dto;

/**
 * 본사 재고 신청 대시보드의 상태별 요약 건수다.
 *
 * <p>{@code approvedCount}는 단순히 승인 직후 상태만 세는 값이 아니라, 대기·반려·취소를
 * 제외한 {@code APPROVED}, {@code PREPARING}, {@code SHIPPING}, {@code DELIVERED} 상태의
 * 신청 수를 뜻한다. 현재 서비스 흐름은 승인 직후 바로 {@code PREPARING}으로 이동한다.</p>
 *
 * @param totalCount 전체 신청 수
 * @param pendingCount 본사 처리를 기다리는 신청 수
 * @param approvedCount 대기·반려·취소를 제외한 승인 후 처리 상태의 신청 수
 * @param rejectedCount 반려된 신청 수
 */
public record StockRequestSummaryResponse(
        long totalCount,
        long pendingCount,
        long approvedCount,
        long rejectedCount
) {
}
