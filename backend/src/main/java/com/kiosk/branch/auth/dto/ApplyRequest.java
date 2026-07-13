package com.kiosk.branch.auth.dto;

public record ApplyRequest(
        String managerName,
        String branchName,
        String address,
        String email,
        String phone,
        String businessNumber,
        String loginId,
        String password
) {
}
