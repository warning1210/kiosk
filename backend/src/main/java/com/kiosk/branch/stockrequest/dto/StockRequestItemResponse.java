package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequestItem;

/** 신청서 한 줄을 화면에 보여 주기 위한 응답. 수량 단위는 통(tub)이다. */
public record StockRequestItemResponse(
        Long flavorId,
        String flavorName,
        Integer requestedQuantity,
        Integer approvedQuantity
) {

    public static StockRequestItemResponse from(StockRequestItem item) {
        return new StockRequestItemResponse(
                item.getFlavor().getFlavorId(),
                item.getFlavor().getFlavorName(),
                item.getRequestedQuantity(),
                item.getApprovedQuantity()
        );
    }
}
