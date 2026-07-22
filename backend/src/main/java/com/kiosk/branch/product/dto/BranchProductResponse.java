package com.kiosk.branch.product.dto;

import com.kiosk.domain.product.Product;

public record BranchProductResponse(
        Long productId,
        String categoryName,
        String productName,
        Integer basePrice,
        String imageUrl,
        Boolean isVisible
) {
    public static BranchProductResponse from(Product product, Boolean branchIsVisible) {
        return new BranchProductResponse(
                product.getProductId(),
                product.getCategory() != null ? product.getCategory().getCategoryName() : "",
                product.getProductName(),
                product.getBasePrice(),
                product.getImageUrl(),
                branchIsVisible != null ? branchIsVisible : product.getIsVisible()
        );
    }
}
