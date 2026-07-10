package com.example.kiosksim.dto;

public record DiscountRequest(
        String couponToken,
        Integer couponDiscountAmount,
        Integer usedPoint
) {
    public int safeCouponDiscountAmount() {
        return couponDiscountAmount == null ? 0 : couponDiscountAmount;
    }

    public int safeUsedPoint() {
        return usedPoint == null ? 0 : usedPoint;
    }
}
