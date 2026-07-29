package com.kiosk.hq.product.dto;

import com.kiosk.domain.product.Product;
import java.time.LocalDateTime;

public record HqProductResponse(
        Long productId,
        Long categoryId,
        String categoryName,
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
        Boolean isVisible,
        LocalDateTime createdAt) {
    public static HqProductResponse from(Product product) {
        return new HqProductResponse(
                product.getProductId(),
                product.getCategory() == null ? null : product.getCategory().getCategoryId(),
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                product.getProductName(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getDescription(),
                product.getRequiresFlavorSelection(),
                product.getSelectableFlavorCount() == null ? null : product.getSelectableFlavorCount().intValue(),
                product.getContainerPolicy().name(),
                product.getIsLarge(),
                product.getIsNew(),
                product.getSaleStatus().name(),
                product.getIsVisible(),
                product.getCreatedAt());
    }
}
