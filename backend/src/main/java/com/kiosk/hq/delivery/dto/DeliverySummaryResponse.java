package com.kiosk.hq.delivery.dto;

/** 배송 관리 화면 상단 카드용 상태별 건수. */
public record DeliverySummaryResponse(
        long preparingCount,
        long shippingCount,
        long deliveredCount,
        long delayedCount
) {
}
