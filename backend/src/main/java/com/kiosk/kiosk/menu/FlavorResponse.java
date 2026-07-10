package com.kiosk.kiosk.menu;

import com.kiosk.domain.flavor.Flavor;

public record FlavorResponse(
        Long flavorId,
        String flavorName,
        String imageUrl,
        String allergyInfo
) {

    public static FlavorResponse from(Flavor flavor) {
        return new FlavorResponse(
                flavor.getFlavorId(),
                flavor.getFlavorName(),
                flavor.getImageUrl(),
                flavor.getAllergyInfo()
        );
    }
}
