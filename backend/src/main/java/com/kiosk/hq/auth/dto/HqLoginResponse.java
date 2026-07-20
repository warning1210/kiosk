package com.kiosk.hq.auth.dto;

public record HqLoginResponse(Long adminId, String name, String token) {
}
