package com.kiosk.kiosk.payment;
 
import com.kiosk.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
 
public record PaymentStatusResponse(
        Long orderId,
        PaymentStatus paymentStatus,
        Integer requestedAmount,
        Integer paidAmount,
        String approvalNumber,
        LocalDateTime paidAt
) {
}
