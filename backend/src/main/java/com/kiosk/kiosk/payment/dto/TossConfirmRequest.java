package com.kiosk.kiosk.payment.dto;

public record TossConfirmRequest(
        String qrToken,
        String paymentKey,
        String orderId,
        Integer amount
) {
}
