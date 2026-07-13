package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApproveItemRequest(
        @NotNull Long flavorId,
        @NotNull @Min(0) Integer approvedQuantity
) {
}
