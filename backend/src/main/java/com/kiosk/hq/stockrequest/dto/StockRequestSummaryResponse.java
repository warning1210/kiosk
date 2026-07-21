package com.kiosk.hq.stockrequest.dto;

/** 본사 대시보드 상단 카드에 쓰는 상태별 건수 (HQ-004). */
public record StockRequestSummaryResponse(
        long totalCount,
        long pendingCount,
        long approvedCount,
        long rejectedCount
) {
}
