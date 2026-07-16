package com.kiosk.branch.auth.dto;

public record JoinRequest(
        String token,
        String loginId,
        String password,
        String managerName,
        String branchName,
        String phone,
        String address,
        String businessNumber
) {
}
