package com.kiosk.kiosk.menu;

import com.kiosk.domain.flavor.Flavor;

public record FlavorResponse(
        Long flavorId,
        String flavorName,
        String description,
        String allergyInfo,
        String imageUrl,
        String saleStatus,
        boolean monthlyFlavor,
        Integer popularityRank
) {
    public static FlavorResponse from(Flavor flavor, boolean monthlyFlavor, Integer popularityRank) {
        return new FlavorResponse(
                flavor.getFlavorId(),
                flavor.getFlavorName(),
                flavor.getDescription(),
                flavor.getAllergyInfo(),
                flavor.getImageUrl(),
                flavor.getSaleStatus().name(),
                monthlyFlavor,
                popularityRank
        );
    }
}
