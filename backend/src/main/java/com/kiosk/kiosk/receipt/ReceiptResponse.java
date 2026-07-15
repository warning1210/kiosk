package com.kiosk.kiosk.receipt;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 화면(그리고 필요하면 인쇄)에 보여줄 "영수증 전체" 데이터.
 * 주문 + 주문상품 + 맛 + 결제 정보를 한 덩어리로 모아서 프론트로 내려준다.
 */
public record ReceiptResponse(
        String storeName,        // 지점명 (예: 베스킨라빈스 강남점)
        String orderNumber,      // 주문번호 (영수증 번호로도 사용)
        Integer waitingNumber,   // 대기번호 (없으면 null)
        String orderType,        // DINE_IN / TAKEOUT
        LocalDateTime paidAt,    // 결제 완료 시각
        List<ReceiptItemResponse> items,
        int amountBeforeDiscount, // 할인 전 금액
        int discountAmount,       // 포인트 사용 등 할인 금액
        int finalAmount,          // 최종 결제 금액
        int earnedPoints,         // 이번 결제로 적립된 포인트
        String paymentMethod,     // QR / CARD / CASH ...
        String approvalNumber     // 결제 승인번호
) {
}
