package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.category.Category;

public record CategoriResponse(
        Long categoryId,
        String categoryName
) {

    public static CategoriResponse from(Category category) {
        return new CategoriResponse(
                category.getCategoryId(),
                category.getCategoryName()
        );
    }
}
