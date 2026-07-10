package com.example.kiosksim.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class KioskOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long branchId;
    private Long kioskId;
    private String orderNo;
    private Integer waitingNo;
    private String orderType;
    private String status;
    private Integer usedPoint;
    private Integer originalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private LocalDateTime orderedAt;

    protected KioskOrder() {
    }

    public KioskOrder(Long branchId, Long kioskId, String orderNo, Integer waitingNo, String orderType,
                      String status, Integer usedPoint, Integer originalAmount, Integer discountAmount,
                      Integer finalAmount, LocalDateTime orderedAt) {
        this.branchId = branchId;
        this.kioskId = kioskId;
        this.orderNo = orderNo;
        this.waitingNo = waitingNo;
        this.orderType = orderType;
        this.status = status;
        this.usedPoint = usedPoint;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.orderedAt = orderedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public Long getKioskId() {
        return kioskId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Integer getWaitingNo() {
        return waitingNo;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getStatus() {
        return status;
    }

    public Integer getUsedPoint() {
        return usedPoint;
    }

    public Integer getOriginalAmount() {
        return originalAmount;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public Integer getFinalAmount() {
        return finalAmount;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }
}
