package com.kiosk.branch.auth.dto;

public record DbLoginRequest(String loginId, String password, String turnstileToken) {
}
