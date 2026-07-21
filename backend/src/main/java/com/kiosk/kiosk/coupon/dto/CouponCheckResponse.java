package com.kiosk.kiosk.coupon.dto;

import java.time.LocalDateTime;

public record CouponCheckResponse(
        String couponName,
        Integer discountAmount,
        LocalDateTime expiresAt
) {
}
