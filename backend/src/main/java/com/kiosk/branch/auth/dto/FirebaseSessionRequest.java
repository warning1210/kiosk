package com.kiosk.branch.auth.dto;

public record FirebaseSessionRequest(String idToken, String turnstileToken) {
}
