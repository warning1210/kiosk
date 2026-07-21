package com.kiosk.hq.coupon.dto;

import com.kiosk.domain.coupon.Coupon;
import java.time.LocalDateTime;

public record HqCouponResponse(
        Long couponId,
        String couponName,
        String qrToken,
        Integer discountAmount,
        String couponStatus,
        Long customerId,
        String customerMobileNumber,
        String customerGrade,
        Long eventId,
        LocalDateTime expiresAt
) {
    public static HqCouponResponse from(Coupon coupon) {
        return new HqCouponResponse(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getQrToken(),
                coupon.getDiscountAmount(),
                coupon.getCouponStatus().name(),
                coupon.getCustomer() != null ? coupon.getCustomer().getCustomerId() : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getMobileNumber() : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getGrade().name() : null,
                coupon.getEvent() != null ? coupon.getEvent().getEventId() : null,
                coupon.getExpiresAt()
        );
    }
}
