package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.product.Product;
import com.kiosk.domain.common.SaleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuProductDto {
    private Long productId;
    private String productName;
    private Integer basePrice;
    private String imageUrl;
    private String description;
    private Boolean requiresFlavorSelection;
    private Integer selectableFlavorCount;
    private Boolean isLarge;
    private Boolean isNew;
    private SaleStatus saleStatus;

    public static MenuProductDto from(Product product) {
        return MenuProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .basePrice(product.getBasePrice())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .requiresFlavorSelection(product.getRequiresFlavorSelection())
                .selectableFlavorCount(product.getSelectableFlavorCount())
                .isLarge(product.getIsLarge())
                .isNew(product.getIsNew())
                .saleStatus(product.getSaleStatus())
                .build();
    }
}
