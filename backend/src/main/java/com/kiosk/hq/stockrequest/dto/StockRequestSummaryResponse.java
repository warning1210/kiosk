package com.kiosk.hq.stockrequest.dto;

public record StockRequestSummaryResponse(
        long totalCount,
        long pendingCount,
        long approvedCount,
        long rejectedCount
) {
}
