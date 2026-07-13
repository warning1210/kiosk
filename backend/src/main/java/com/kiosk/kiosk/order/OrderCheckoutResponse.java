package com.kiosk.kiosk.order;

public record OrderCheckoutResponse(
        Long orderId,
        String orderNumber,
        Integer amountBeforeDiscount,
        Integer discountAmount,
        Integer finalAmount
) {
}
