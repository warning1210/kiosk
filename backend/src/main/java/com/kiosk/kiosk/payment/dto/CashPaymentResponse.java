package com.kiosk.kiosk.payment.dto;

/**
 * 현금 주문 등록 결과. waitingNumber는 현금 주문끼리 매일 1번부터 시작한다.
 */
public record CashPaymentResponse(
        Long orderId,
        Integer waitingNumber,
        String paymentMethod,
        String paymentStatus
) {
}
