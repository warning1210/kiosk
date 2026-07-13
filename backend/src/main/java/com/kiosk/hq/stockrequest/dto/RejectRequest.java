package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank(message = "반려 사유를 입력해주세요") String rejectionReason
) {
}
