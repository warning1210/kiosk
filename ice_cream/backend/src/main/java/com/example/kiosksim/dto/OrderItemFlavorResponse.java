package com.example.kiosksim.dto;

public record OrderItemFlavorResponse(
        Long id,
        Long flavorId,
        String flavorNameSnapshot,
        Integer selectOrder,
        Integer quantity
) {
}
