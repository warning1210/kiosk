package com.kiosk.kiosk.receipt;

import java.util.List;

/**
 * 영수증에 찍히는 "상품 한 줄"과 그 아래 선택 옵션/맛.
 * (예: 파인트  12,000원  /  컵  /  초코, 바닐라, 민트)
 */
public record ReceiptItemResponse(
        String productName,
        int quantity,
        int lineTotal,
        String containerType,   // CUP / CONE / NONE
        int spoonCount,
        Integer dryIceMinutes,  // 없으면 null
        List<ReceiptFlavorResponse> flavors
) {
}
