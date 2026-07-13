package com.kiosk.branch.auth.dto;

import com.kiosk.domain.branchapplication.BranchApplication;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long applicationId,
        String managerName,
        String branchName,
        String address,
        String email,
        String phone,
        String businessNumber,
        String loginId,
        String accountStatus,
        String status,
        LocalDateTime appliedAt,
        String inviteUrl
) {
    public static ApplicationResponse of(BranchApplication application, String loginId, String accountStatus, String inviteUrl) {
        return new ApplicationResponse(
                application.getBranchApplicationId(),
                application.getManagerName(),
                application.getBranchName(),
                application.getAddress(),
                application.getEmail(),
                application.getPhone(),
                application.getBusinessNumber(),
                loginId,
                accountStatus,
                application.getApprovalStatus().name(),
                application.getAppliedAt(),
                inviteUrl
        );
    }
}
