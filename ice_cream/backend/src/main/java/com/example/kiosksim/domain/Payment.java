package com.example.kiosksim.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String paymentMethod;
    private String status;  // PENDING, DONE, FAILED
    private String qrToken;
    private LocalDateTime qrExpiresAt;
    private Integer paidAmount;
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;  // 결제한 회원

    private Integer usedPoints;  // 결제에 사용한 포인트
    private Integer earnedPoints;  // 결제로 적립된 포인트

    protected Payment() {
    }

    public Payment(Long orderId, String paymentMethod, String status, String qrToken,
                   LocalDateTime qrExpiresAt, Integer paidAmount, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.qrToken = qrToken;
        this.qrExpiresAt = qrExpiresAt;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
        this.usedPoints = 0;
        this.earnedPoints = 0;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public String getQrToken() {
        return qrToken;
    }

    public LocalDateTime getQrExpiresAt() {
        return qrExpiresAt;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Integer getUsedPoints() {
        return usedPoints;
    }

    public void setUsedPoints(Integer usedPoints) {
        this.usedPoints = usedPoints;
    }

    public Integer getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(Integer earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    // 결제 완료 처리
    public void completePayment() {
        this.status = "DONE";
        this.paidAt = LocalDateTime.now();
    }

    // 결제 실패 처리
    public void failPayment() {
        this.status = "FAILED";
    }
}
