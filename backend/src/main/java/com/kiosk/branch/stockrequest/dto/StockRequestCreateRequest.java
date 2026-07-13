package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.Urgency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StockRequestCreateRequest(
        String requestReason,
        Urgency urgency,
        @NotEmpty(message = "신청할 상품을 1개 이상 선택해주세요") @Valid List<StockRequestItemRequest> items
) {
}
