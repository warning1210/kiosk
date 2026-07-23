package com.kiosk.kiosk.payment.dto;

import java.time.LocalDateTime;

public record PaymentQrResponse(
        Long orderId,
        String qrToken,
        LocalDateTime expiresAt,
        Integer requestedAmount,
        String checkoutBaseUrl
) {
}
