package com.kiosk.kiosk.payment;

import com.kiosk.domain.payment.PaymentStatus;

public record PaymentStatusResponse(Long orderId, PaymentStatus paymentStatus, Integer requestedAmount) {
}

