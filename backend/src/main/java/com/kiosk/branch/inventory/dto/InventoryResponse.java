package com.kiosk.branch.inventory.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long inventoryId,
        Long flavorId,
        String flavorName,
        String imageUrl,
        Integer remainingGrams,
        String status,
        String requestStatus,
        LocalDateTime expectedArrivalAt,
        LocalDateTime updatedAt
) {
}
