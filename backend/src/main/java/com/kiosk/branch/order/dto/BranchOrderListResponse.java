package com.kiosk.branch.order.dto;

import com.kiosk.domain.order.OrderType;
import com.kiosk.domain.order.OrderStatus;
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
}
