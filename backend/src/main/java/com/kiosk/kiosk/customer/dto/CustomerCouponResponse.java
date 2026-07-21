package com.kiosk.kiosk.customer.dto;

import com.kiosk.domain.coupon.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerCouponResponse(
        Long couponId,
        String couponName,
        String qrToken,
        String discountType,
        BigDecimal discountRate,
        Integer discountAmount,
        LocalDateTime expiresAt
) {
    public static CustomerCouponResponse from(Coupon coupon) {
        return new CustomerCouponResponse(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getQrToken(),
                coupon.getDiscountType().name(),
                coupon.getDiscountRate(),
                coupon.getDiscountAmount(),
                coupon.getExpiresAt()
        );
    }
}
