package com.kiosk.hq.product.dto;

public record HqProductUpsertRequest(
        Long categoryId,
        String productName,
        Integer basePrice,
        String imageUrl,
        String description,
        Boolean requiresFlavorSelection,
        Integer selectableFlavorCount,
        String containerPolicy,
        Boolean isLarge,
        Boolean isNew,
        String saleStatus,
        Boolean isVisible
) {
}
