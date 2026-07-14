package com.kiosk.hq.auth.repository;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RegistrationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean isUsed = false; // 1회성이면 사용 여부 체크, 기간 내 무제한이면 제외 가능

    public RegistrationToken(String token, int expiryHours) {
        this.token = token;
        this.expiryDate = LocalDateTime.now().plusHours(expiryHours);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
    
    public void useToken() {
        this.isUsed = true;
    }
}
