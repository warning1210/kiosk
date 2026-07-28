package com.kiosk.branch.order.dto;

import com.kiosk.domain.order.OrderType;
import com.kiosk.domain.order.OrderStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchOrderListResponse {
    private Long orderId;
    private String orderNumber;
    private Integer waitingNumber;
    private long elapsedMinutes;
    private OrderType orderType;
    private String menuSummary;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private Integer finalAmount;
    private String paymentMethod;
}
