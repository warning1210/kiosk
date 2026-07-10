package com.example.kiosksim.dto;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        Integer basePrice,
        Boolean requiresFlavor,
        Integer selectableFlavorCount,
        Boolean bulk
) {
}
