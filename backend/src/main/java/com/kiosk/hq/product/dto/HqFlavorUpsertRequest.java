package com.kiosk.hq.product.dto;

public record HqFlavorUpsertRequest(
        Long categoryId,
        String flavorName,
        String imageUrl,
        String description,
        String allergyInfo,
        String saleStatus,
        Boolean isVisible
) {
}
