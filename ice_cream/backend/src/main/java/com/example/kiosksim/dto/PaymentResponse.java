package com.example.kiosksim.dto;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentMethod,
        String status,
        String qrToken,
        LocalDateTime qrExpiresAt,
        Integer paidAmount,
        LocalDateTime paidAt
) {
}
