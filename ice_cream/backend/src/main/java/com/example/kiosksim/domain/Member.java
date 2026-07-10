package com.example.kiosksim.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;  // 전화번호 (회원 식별자)

    @Column(nullable = false)
    private Integer totalPoints;  // 누적 포인트

    @Column(nullable = false)
    private Integer availablePoints;  // 사용 가능 포인트

    private LocalDateTime createdAt;  // 가입일
    private LocalDateTime updatedAt;  // 마지막 수정일

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 포인트 추가
    public void addPoints(Integer points) {
        this.totalPoints += points;
        this.availablePoints += points;
    }

    // 포인트 사용
    public boolean usePoints(Integer points) {
        if (this.availablePoints >= points) {
            this.availablePoints -= points;
            return true;
        }
        return false;
    }
}
