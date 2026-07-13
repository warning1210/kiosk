package com.kiosk.branch.order.dto;

import com.kiosk.domain.order.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderStatusUpdateRequest {
    private OrderStatus status;
    private String cancelReason;
}
