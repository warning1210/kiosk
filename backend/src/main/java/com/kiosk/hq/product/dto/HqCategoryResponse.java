package com.kiosk.hq.product.dto;

import com.kiosk.domain.category.Category;

public record HqCategoryResponse(Long categoryId, String categoryName) {
    public static HqCategoryResponse from(Category category) {
        return new HqCategoryResponse(category.getCategoryId(), category.getCategoryName());
    }
}
