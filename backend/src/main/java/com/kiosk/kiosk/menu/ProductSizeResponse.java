package com.kiosk.kiosk.menu;

import com.kiosk.domain.product.Product;

public record ProductSizeResponse(
        Long productId,
        String productName,
        Integer basePrice,
        Integer selectableFlavorCount,
        String containerPolicy
) {
    public static ProductSizeResponse from(Product product) {
        String displayName = "하프갤런".equals(product.getProductName())
                ? "하프갤론"
                : product.getProductName();
        int flavorCount = "하프갤론".equals(displayName)
                ? 6
                : product.getSelectableFlavorCount();
        return new ProductSizeResponse(
                product.getProductId(),
                displayName,
                product.getBasePrice(),
                flavorCount,
                product.getContainerPolicy().name()
        );
    }
}
