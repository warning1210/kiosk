package com.kiosk.branch.auth.dto;

// 토큰은 여기 담지 않는다 - httpOnly 쿠키(AuthCookie)로만 내려간다. 자세한 이유는 AuthCookie 참고.
public record DbLoginResponse(Long adminId, Long branchId, String branchName, String managerName) {
}
