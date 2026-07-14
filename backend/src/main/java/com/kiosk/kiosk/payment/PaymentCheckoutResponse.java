package com.kiosk.kiosk.payment;
 
/**
 * QR을 스캔해서 열리는 결제 페이지가 토스 SDK를 초기화할 때 필요한 정보.
 * orderId는 qrToken을 그대로 재사용한다 (QR 재발급 = 새 주문번호).
 */
public record PaymentCheckoutResponse(
        String orderId,      // = qrToken
        String orderName,
        Integer amount,
        String clientKey,
        String successUrl,
        String failUrl
) {
}

 