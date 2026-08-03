package com.kiosk.hq.auth.dto;

public record HqLoginRequest(String loginId, String password, String turnstileToken) {
}
