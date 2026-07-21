package com.kiosk.hq.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HqCouponIssueRequest(
        String couponName,
        String grade, // FRIEND | FAMILY | VIP
        String discountType, // RATE | AMOUNT
        BigDecimal discountRate,
        Integer discountAmount,
        LocalDateTime expiresAt,
        Long eventId
) {
}
