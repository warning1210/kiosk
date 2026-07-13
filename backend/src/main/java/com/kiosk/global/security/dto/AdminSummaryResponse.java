package com.kiosk.global.security.dto;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;

public record AdminSummaryResponse(
        Long adminId,
        String name,
        AdminRole role,
        Long branchId,
        String branchName
) {

    public static AdminSummaryResponse from(Admin admin) {
        var branch = admin.getBranch();
        return new AdminSummaryResponse(
                admin.getAdminId(),
                admin.getName(),
                admin.getRole(),
                branch != null ? branch.getBranchId() : null,
                branch != null ? branch.getBranchName() : null
        );
    }
}
