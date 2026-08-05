package com.kiosk.domain.order;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.common.Language;
import com.kiosk.domain.customer.Customer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table name is the reserved word "order"; mapper SQL must quote it with backticks.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    private Long orderId;

    private Branch branch;

    private String kioskCode;

    private Customer customer;

    private String orderNumber;

    private Integer waitingNumber;

    private OrderType orderType;

    private LocalDateTime pickupAt;

    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

    @Builder.Default
    private Language language = Language.ko;

    @Builder.Default
    private Boolean isEasyMode = false;

    @Builder.Default
    private Integer usedPoints = 0;

    @Builder.Default
    private Integer earnedPoints = 0;

    private Integer amountBeforeDiscount;

    @Builder.Default
    private Integer discountAmount = 0;

    private Integer finalAmount;

    private String cancellationReason;

    private LocalDateTime createdAt;

    private LocalDateTime orderCompletedAt;

    // 吏??二쇰Ц 愿由??붾㈃?먯꽌 二쇰Ц???닿릿 ?곹뭹 紐⑸줉??諛붾줈 議고쉶?섍린 ?꾪븳 ?몄쓽 ?곌?愿怨?    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
