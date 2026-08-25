package com.kiosk.hq.auth.dto;

// 토큰은 여기 담지 않는다 - httpOnly 쿠키(AuthCookie)로만 내려간다. 본문에 실으면 프론트가
// 그 값을 읽어 localStorage에 저장하게 되고, 그 순간 XSS로 유출 가능한 위치로 되돌아간다.
public record HqLoginResponse(Long adminId, String name) {
}
