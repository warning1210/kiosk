package com.kiosk.domain.order;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.common.Language;
import com.kiosk.domain.customer.Customer;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table name is the reserved word "order" - must stay backtick-quoted so Hibernate
 * generates valid MySQL DDL/DML against it.
 */
@Entity
@Table(name = "`order`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "kiosk_code", length = 50)
    private String kioskCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "order_number", length = 30, unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "waiting_number")
    private Integer waitingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    @Builder.Default
    private Language language = Language.ko;

    @Column(name = "is_easy_mode", nullable = false)
    @Builder.Default
    private Boolean isEasyMode = false;

    @Column(name = "used_points", nullable = false)
    @Builder.Default
    private Integer usedPoints = 0;

    @Column(name = "earned_points", nullable = false)
    @Builder.Default
    private Integer earnedPoints = 0;

    @Column(name = "amount_before_discount", nullable = false)
    private Integer amountBeforeDiscount;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Integer discountAmount = 0;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "order_completed_at")
    private LocalDateTime orderCompletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
