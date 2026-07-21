package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.Urgency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 지점이 본사에 올리는 재고 신청서.
 *
 * <p>@Valid를 붙여야 안에 든 items 각 줄의 검증까지 함께 돌아간다.
 */
public record StockRequestCreateRequest(
        String requestReason,
        Urgency urgency,
        @NotEmpty(message = "신청할 상품을 1개 이상 선택해주세요")
        @Valid List<StockRequestItemRequest> items
) {
}
