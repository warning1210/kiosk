package com.kiosk.branch.auth.dto;

public record LoginResponse(Long adminId, Long branchId, String branchName, String managerName) {
}
