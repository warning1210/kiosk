package com.kiosk.domain.payment;

import com.kiosk.domain.order.Order;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private Long paymentId;

    private Order order;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.QR;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.QR_CREATED;

    private String qrToken;

    private LocalDateTime qrExpiresAt;

    private Integer requestedAmount;

    @Builder.Default
    private Integer paidAmount = 0;

    private String approvalNumber;

    private String failureReason;

    private LocalDateTime paidAt;

    private String paymentKey;
}
