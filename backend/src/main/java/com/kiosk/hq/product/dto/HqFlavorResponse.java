package com.kiosk.hq.product.dto;

import com.kiosk.domain.flavor.Flavor;
import java.time.LocalDateTime;

public record HqFlavorResponse(
        Long flavorId,
        Long categoryId,
        String categoryName,
        String flavorName,
        String imageUrl,
        String description,
        String allergyInfo,
        String saleStatus,
        Boolean isVisible,
        LocalDateTime createdAt
) {
    public static HqFlavorResponse from(Flavor flavor) {
        return new HqFlavorResponse(
                flavor.getFlavorId(),
                flavor.getCategory() == null ? null : flavor.getCategory().getCategoryId(),
                flavor.getCategory() == null ? null : flavor.getCategory().getCategoryName(),
                flavor.getFlavorName(),
                flavor.getImageUrl(),
                flavor.getDescription(),
                flavor.getAllergyInfo(),
                flavor.getSaleStatus().name(),
                flavor.getIsVisible(),
                flavor.getCreatedAt()
        );
    }
}
