package com.example.kiosksim.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        Integer waitingNo,
        String orderType,
        String status,
        Integer originalAmount,
        Integer discountAmount,
        Integer finalAmount,
        LocalDateTime orderedAt,
        List<OrderItemResponse> items,
        PaymentResponse payment
) {
}
