package com.example.kiosksim.dto;

import java.util.List;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productNameSnapshot,
        Integer quantity,
        Integer unitPrice,
        Integer lineTotal,
        String containerType,
        Integer spoonCount,
        Integer dryIceMinutes,
        List<OrderItemFlavorResponse> flavors
) {
}
