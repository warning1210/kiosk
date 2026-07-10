package com.example.kiosksim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CartFlavorRequest(
        @NotNull Long flavorId,
        @NotBlank String flavorName,
        @NotNull Integer selectOrder
) {
}
