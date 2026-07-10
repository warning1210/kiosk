package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.category.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MenuCategoryDto {
    private Long categoryId;
    private String categoryName;
    private List<MenuProductDto> products;
    private List<MenuFlavorDto> flavors;

    public static MenuCategoryDto of(Category category, List<MenuProductDto> products, List<MenuFlavorDto> flavors) {
        return MenuCategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .products(products)
                .flavors(flavors)
                .build();
    }
}
