package com.kiosk.branch.order.dto;

import com.kiosk.domain.order.OrderStatus;
import com.kiosk.domain.order.OrderType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchOrderDetailResponse {
    private Long orderId;
    private String orderNumber;
    private Integer waitingNumber;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private Integer amountBeforeDiscount;
    private Integer discountAmount;
    private Integer finalAmount;
    
    private BranchOrderDetailPaymentResponse payment;
    private List<BranchOrderDetailItemResponse> items;
}
