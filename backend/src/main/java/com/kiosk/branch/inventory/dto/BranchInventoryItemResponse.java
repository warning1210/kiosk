package com.kiosk.branch.inventory.dto;

import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.InventoryStatus;

public record BranchInventoryItemResponse(
        Long branchInventoryId,
        Long flavorId,
        String flavorName,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Integer currentQuantity,
        Integer safetyQuantity,
        InventoryStatus inventoryStatus,
        Boolean isKioskVisible
) {

    public static BranchInventoryItemResponse from(BranchInventory inventory) {
        var flavor = inventory.getFlavor();
        var category = flavor.getCategory();
        return new BranchInventoryItemResponse(
                inventory.getBranchInventoryId(),
                flavor.getFlavorId(),
                flavor.getFlavorName(),
                flavor.getImageUrl(),
                category != null ? category.getCategoryId() : null,
                category != null ? category.getCategoryName() : null,
                inventory.getCurrentQuantity(),
                inventory.getSafetyQuantity(),
                inventory.getInventoryStatus(),
                inventory.getIsKioskVisible()
        );
    }
}
