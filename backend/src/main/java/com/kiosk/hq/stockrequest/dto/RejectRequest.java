package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 본사가 재고 신청을 반려할 때 받는 JSON 본문이다.
 *
 * @param rejectionReason 화면에서 지점에 보여 줄 반려 사유. 공백만 입력할 수 없다.
 */
public record RejectRequest(
        @NotBlank(message = "반려 사유를 입력해주세요") String rejectionReason
) {
}
