package com.kiosk.branch.stockrequest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 신청서에 담기는 맛 한 줄. 수량 단위는 통(tub)이다. */
public record StockRequestItemRequest(
        @NotNull(message = "맛을 선택해주세요") Long flavorId,
        @NotNull(message = "신청 수량을 입력해주세요")
        @Min(value = 1, message = "신청 수량은 1개 이상이어야 합니다") Integer requestedQuantity
) {
}
