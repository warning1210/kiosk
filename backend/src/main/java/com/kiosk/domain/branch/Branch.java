package com.kiosk.domain.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_name", length = 100, nullable = false)
    private String branchName;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "manager_name", length = 50)
    private String managerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_status", nullable = false)
    @Builder.Default
    private OperationStatus operationStatus = OperationStatus.PENDING;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "is_busy", nullable = false)
    @Builder.Default
    private Boolean isBusy = false;

    @Column(name = "estimated_wait_minutes", columnDefinition = "tinyint")
    private Byte estimatedWaitMinutes;

    @Column(name = "kiosk_code", length = 50)
    private String kioskCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kiosk_status", nullable = false)
    @Builder.Default
    private KioskStatus kioskStatus = KioskStatus.ACTIVE;

    @Column(name = "kiosk_last_access_at")
    private LocalDateTime kioskLastAccessAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
