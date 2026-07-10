package com.example.kiosksim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartItemRequest(
        String clientItemId,
        @NotNull Long productId,
        @NotBlank String productName,
        @NotNull Boolean requiresFlavor,
        @NotNull Integer selectableFlavorCount,
        @NotNull @Min(1) Integer quantity,
        @NotNull Integer unitPrice,
        @NotBlank String containerType,
        @NotNull Integer spoonCount,
        @NotNull Integer dryIceMinutes,
        List<@Valid CartFlavorRequest> flavors
) {
}
