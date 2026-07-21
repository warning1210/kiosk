package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.event.Event;
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
        Boolean isNew,
        Long sizeUpToProductId,
        String sizeUpToProductName,
        Integer sizeUpAdditionalPayment
) {

    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    // sizeUpEvent: 이 상품(product)이 SIZE_UP 이벤트의 sizeUpFromProduct일 때만 넘어온다(MenuService에서 매칭)
    public static ProductResponse from(Product product, Event sizeUpEvent) {
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
                product.getIsNew(),
                sizeUpEvent != null ? sizeUpEvent.getSizeUpToProduct().getProductId() : null,
                sizeUpEvent != null ? sizeUpEvent.getSizeUpToProduct().getProductName() : null,
                sizeUpEvent != null ? sizeUpEvent.getAdditionalPayment() : null
        );
    }
}
