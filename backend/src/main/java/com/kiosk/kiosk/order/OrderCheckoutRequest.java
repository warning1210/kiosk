package com.kiosk.kiosk.order;

import com.kiosk.domain.common.Language;
import com.kiosk.domain.order.OrderType;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCheckoutRequest(
        Long branchId,
        OrderType orderType,
        LocalDateTime pickupAt,
        String customerMobileNumber,
        Integer usedPoints,
        Language language,
        List<OrderItemRequest> items
) {
}
