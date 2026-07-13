package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * itemOverrides가 비어있으면 모든 항목이 신청 수량 그대로 승인된다.
 */
public record ApproveRequest(
        @Valid List<ApproveItemRequest> itemOverrides
) {
}
