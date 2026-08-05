package com.kiosk.domain.branchapplication;

import com.kiosk.domain.admin.Admin;
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
public class BranchApplication {

    private Long branchApplicationId;

    private String branchName;

    private String managerName;

    private String phone;

    private String email;

    private String address;

    private String businessNumber;

    private String inviteToken;

    private LocalDateTime inviteExpiresAt;

    private Admin issuedByAdmin;

    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private String rejectionReason;

    private Admin processedAdmin;

    private LocalDateTime processedAt;

    private Branch approvedBranch;

    private LocalDateTime appliedAt;

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
