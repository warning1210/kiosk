package com.kiosk.domain.coupon;

import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_name", length = 100, nullable = false)
    private String couponName;

    @Column(name = "qr_token", unique = true, nullable = false)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    @Builder.Default
    private CouponDiscountType discountType = CouponDiscountType.AMOUNT;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", nullable = false)
    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_order_id")
    private Order usedOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 정률(RATE)이면 주문 금액 대비 %로, 정액(AMOUNT)이면 고정 금액으로 할인액을 계산한다
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
