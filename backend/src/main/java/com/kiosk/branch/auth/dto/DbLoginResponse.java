package com.kiosk.branch.auth.dto;

public record DbLoginResponse(Long adminId, Long branchId, String branchName, String managerName, String token) {
}
