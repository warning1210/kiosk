package com.kiosk.hq.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HqProductUpsertRequest(
                Long categoryId,

                @NotBlank(message = "상품명을 입력해주세요.") String productName,

                @NotNull(message = "가격을 입력해주세요.") @Min(value = 0, message = "가격은 0 이상이어야 합니다.") Integer basePrice,

                String imageUrl,
                String description,
                Boolean requiresFlavorSelection,
                Integer selectableFlavorCount,
                String containerPolicy,
                Boolean isLarge,
                Boolean isNew,
                String saleStatus,
                Boolean isVisible) {
}
