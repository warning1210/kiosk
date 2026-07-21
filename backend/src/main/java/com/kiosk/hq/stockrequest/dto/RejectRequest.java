package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;

/** 반려는 사유가 반드시 있어야 지점이 무엇을 고쳐 재신청할지 알 수 있다 (HQ-003). */
public record RejectRequest(
        @NotBlank(message = "반려 사유를 입력해주세요") String rejectionReason
) {
}
