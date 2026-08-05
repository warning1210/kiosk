package com.kiosk.domain.admin;

import com.kiosk.domain.branch.Branch;
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
public class Admin {

    private Long adminId;

    private Branch branch;

    private String loginId;

    private String passwordHash;

    private String name;

    private String phone;

    private String email;

    private AdminRole role;

    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    private String inviteToken;

    private LocalDateTime inviteExpiresAt;

    private Admin inviterAdmin;

    private LocalDateTime lastLoginAt;

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
