package com.kiosk.kiosk.menu;

import com.kiosk.domain.product.ContainerPolicy;
import com.kiosk.domain.product.Product;

public record ProductResponse(
        Long productId,
        Long categoryId,
        String productName,
        Integer basePrice,
        String imageUrl,
        String description,
        Boolean requiresFlavorSelection,
        Integer selectableFlavorCount,
        ContainerPolicy containerPolicy,
        Boolean isLarge,
        Boolean isNew
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getProductName(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getDescription(),
                product.getRequiresFlavorSelection(),
                product.getSelectableFlavorCount() != null ? product.getSelectableFlavorCount().intValue() : null,
                product.getContainerPolicy(),
                product.getIsLarge(),
                product.getIsNew()
        );
    }
}
