package com.kiosk.kiosk.order.dto;

public record OrderCheckoutResponse(
        Long orderId,
        String orderNumber,
        Integer amountBeforeDiscount,
        Integer discountAmount,
        Integer finalAmount
) {
}
