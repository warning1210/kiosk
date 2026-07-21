package com.kiosk.branch.order.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchOrderDetailPaymentResponse {
    private Integer requestedAmount;
    private Integer paidAmount;
    private String paymentMethod;
    private String approvalNumber;
    private LocalDateTime paidAt;
    private String paymentStatus;
    private String installment;
    private String cardNumber;
}
