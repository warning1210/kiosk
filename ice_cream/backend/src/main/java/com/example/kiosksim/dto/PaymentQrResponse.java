package com.example.kiosksim.dto;

public record PaymentQrResponse(
    String qrCode,
    Long orderId,
    Integer amount,
    String status,
    String expiresAt,
    String checkoutUrl
) {}
