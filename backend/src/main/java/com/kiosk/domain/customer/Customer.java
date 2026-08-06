package com.kiosk.domain.customer;

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
public class Customer {

    private Long customerId;

    // 전화번호는 평문으로 들고 있지 않는다 - hash는 조회 전용, masked는 화면 표시용(뒤 4자리만 노출).
    private String mobileNumberHash;

    private String mobileNumberMasked;

    @Builder.Default
    private Integer pointBalance = 0;

    @Builder.Default
    private CustomerGrade grade = CustomerGrade.FRIEND;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
