package com.example.kiosksim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderDraftRequest(
        @NotNull Long branchId,
        @NotNull Long kioskId,
        @NotBlank String orderType,
        @NotEmpty List<@Valid CartItemRequest> cartItems,
        @Valid DiscountRequest discount
) {
}
