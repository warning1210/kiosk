package com.kiosk.kiosk.payment;

import java.time.LocalDateTime;

public record PaymentQrResponse(Long orderId, String qrToken, LocalDateTime expiresAt, Integer requestedAmount) {
}

