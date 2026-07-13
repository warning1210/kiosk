package com.kiosk.kiosk.payment.dto;

import com.kiosk.domain.payment.PaymentStatus;

public record PaymentStatusResponse(Long orderId, PaymentStatus paymentStatus, Integer requestedAmount) {
}
