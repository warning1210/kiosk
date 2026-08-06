package com.kiosk.hq.coupon.dto;

import com.kiosk.domain.coupon.Coupon;
import com.kiosk.global.security.MobileNumberCrypto;
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
    // 본점 관리자가 쿠폰이 어느 고객 것인지 식별해야 해서, 여기서만 예외적으로 전화번호를 복호화해 보여준다.
    public static HqCouponResponse from(Coupon coupon, MobileNumberCrypto mobileNumberCrypto) {
        return new HqCouponResponse(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getQrToken(),
                coupon.getDiscountType().name(),
                coupon.getDiscountRate(),
                coupon.getDiscountAmount(),
                coupon.getCouponStatus().name(),
                coupon.getCustomer() != null ? coupon.getCustomer().getCustomerId() : null,
                coupon.getCustomer() != null ? mobileNumberCrypto.decrypt(coupon.getCustomer().getMobileNumberEnc()) : null,
                coupon.getCustomer() != null ? coupon.getCustomer().getGrade().name() : null,
                coupon.getExpiresAt()
        );
    }
}
