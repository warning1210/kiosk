package com.example.kiosksim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentQrRequest(
    @NotNull(message = "orderId는 필수입니다")
    Long orderId,
    
    @Positive(message = "amount는 양수여야 합니다")
    Integer amount
) {}
