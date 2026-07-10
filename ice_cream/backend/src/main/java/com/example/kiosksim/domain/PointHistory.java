package com.example.kiosksim.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private KioskOrder order;  // 관련 주문 (NULL 가능)

    @Column(nullable = false)
    private Integer pointAmount;  // 포인트 양 (+면 적립, -면 사용)

    @Column(nullable = false)
    private String type;  // "EARNED" (적립), "USED" (사용)

    @Column(nullable = false)
    private String reason;  // "Order Payment" (주문 결제), "Point Usage" (포인트 사용) 등

    @Column(nullable = false)
    private Integer balanceAfter;  // 처리 후 남은 포인트

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
