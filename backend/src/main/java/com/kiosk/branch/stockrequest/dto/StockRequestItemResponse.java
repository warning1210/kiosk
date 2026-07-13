package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequestItem;

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
