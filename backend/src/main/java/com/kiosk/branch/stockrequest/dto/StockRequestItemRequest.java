package com.kiosk.branch.stockrequest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 재고 신청 안에 들어가는 맛 하나와 신청 수량을 표현한다.
 *
 * <p>Bean Validation으로 필수값과 최소 수량을 HTTP 경계에서 확인한다. 실제 맛이
 * DB에 존재하는지와 한 신청 안에서 중복됐는지는 저장소가 필요한 업무 검증이므로
 * 서비스가 담당한다.</p>
 *
 * @param flavorId 신청할 맛 상품의 기본키
 * @param requestedQuantity 신청 수량. 1개 이상이어야 한다.
 */
public record StockRequestItemRequest(
        @NotNull(message = "맛을 선택해주세요") Long flavorId,
        @NotNull(message = "신청 수량을 입력해주세요") @Min(value = 1, message = "신청 수량은 1개 이상이어야 합니다") Integer requestedQuantity
) {
}
