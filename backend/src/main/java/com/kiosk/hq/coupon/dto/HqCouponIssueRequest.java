package com.kiosk.hq.coupon.dto;

import java.time.LocalDateTime;

public record HqCouponIssueRequest(
        String couponName,
        String grade, // FRIEND | FAMILY | VIP
        Integer discountAmount,
        LocalDateTime expiresAt,
        Long eventId
) {
}
