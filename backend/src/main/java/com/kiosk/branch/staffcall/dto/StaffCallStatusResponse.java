package com.kiosk.branch.staffcall.dto;

import java.time.LocalDateTime;

public record StaffCallStatusResponse(Boolean called, LocalDateTime calledAt) {
}
