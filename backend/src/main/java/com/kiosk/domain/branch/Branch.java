package com.kiosk.domain.branch;

import java.time.LocalDate;
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
public class Branch {

    private Long branchId;

    private String branchName;

    private String region;

    private String address;

    private String phone;

    private String email;

    private String managerName;

    @Builder.Default
    private OperationStatus operationStatus = OperationStatus.PENDING;

    private LocalDate openingDate;

    @Builder.Default
    private Boolean isBusy = false;

    private Byte estimatedWaitMinutes;

    private String kioskCode;

    @Builder.Default
    private KioskStatus kioskStatus = KioskStatus.ACTIVE;

    private LocalDateTime kioskLastAccessAt;

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
