package com.kiosk.hq.coupon.dto;

import com.kiosk.domain.coupon.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HqCouponResponse(
        Long couponId,
        String couponName,
        String qrToken,
        String discountType,
        BigDecimal discountRate,
        Integer discountAmount,
        String couponStatus,
        Long customerId,
        String customerMobileNumber,
        String customerGrade,
        LocalDateTime expiresAt
) {
    public static HqCouponResponse from(Coupon coupon) {
        return new HqCouponResponse(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getQrToken(),
                coupon.getDiscountType().name(),
                coupon.getDiscountRate(),
                coupon.getDiscountAmount(),
                coupon.getCouponStatus().name(),
                coupon.getCustomer() != null ? coupon.getCustomer().getCustomerId() : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getMobileNumber() : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getGrade().name() : null,
                coupon.getExpiresAt()
        );
    }
}
