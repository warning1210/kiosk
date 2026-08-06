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
    // 본점 관리자가 쿠폰이 어느 고객 것인지 식별해야 해서, 마스킹된 전화번호(뒤 4자리만 노출)를 보여준다.
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
                coupon.getCustomer() != null ? coupon.getCustomer().getMobileNumberMasked() : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getGrade().name() : null,
                coupon.getExpiresAt()
        );
    }
}
