package com.kiosk.kiosk.receipt;

/**
 * 영수증에 찍히는 "맛 한 줄" (예: 초코, 바닐라)
 */
public record ReceiptFlavorResponse(
        String flavorName,
        int quantity
) {
}
