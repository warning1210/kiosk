package com.kiosk.domain.coupon;

import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.order.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    private Long couponId;

    private String couponName;

    private String qrToken;

    @Builder.Default
    private CouponDiscountType discountType = CouponDiscountType.AMOUNT;

    private BigDecimal discountRate;

    private Integer discountAmount;

    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.AVAILABLE;

    private Order usedOrder;

    private Customer customer;

    private LocalDateTime expiresAt;

    // ?뺣쪧(RATE)?대㈃ 二쇰Ц 湲덉븸 ?鍮?%濡? ?뺤븸(AMOUNT)?대㈃ 怨좎젙 湲덉븸?쇰줈 ?좎씤?≪쓣 怨꾩궛?쒕떎
    public int calculateDiscount(int amountBeforeDiscount) {
        if (discountType == CouponDiscountType.RATE) {
            return discountRate == null ? 0
                    : discountRate.multiply(BigDecimal.valueOf(amountBeforeDiscount))
                            .divide(BigDecimal.valueOf(100))
                            .setScale(0, java.math.RoundingMode.HALF_UP)
                            .intValue();
        }
        return discountAmount != null ? discountAmount : 0;
    }
}
