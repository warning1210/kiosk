package com.kiosk.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequestItem;

/**
 * 재고 신청 응답 안에서 맛 하나의 신청량과 승인량을 나타낸다.
 *
 * <p>승인 전에는 {@code approvedQuantity}가 {@code null}일 수 있으며, 현재 정책에서는
 * 본사가 승인할 때 신청 수량 전체가 승인 수량으로 복사된다.</p>
 *
 * @param flavorId 맛 상품 기본키
 * @param flavorName 화면에 표시할 맛 이름
 * @param requestedQuantity 지점이 신청한 수량
 * @param approvedQuantity 본사가 승인한 수량
 */
public record StockRequestItemResponse(
        Long flavorId,
        String flavorName,
        Integer requestedQuantity,
        Integer approvedQuantity
) {

    /**
     * 신청 품목 엔티티에서 API에 필요한 값만 복사한다.
     *
     * @param item 변환할 신청 품목 엔티티
     * @return 맛 정보와 신청·승인 수량을 담은 응답
     */
    public static StockRequestItemResponse from(StockRequestItem item) {
        return new StockRequestItemResponse(
                item.getFlavor().getFlavorId(),
                item.getFlavor().getFlavorName(),
                item.getRequestedQuantity(),
                item.getApprovedQuantity()
        );
    }
}
