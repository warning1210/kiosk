package com.kiosk.kiosk.payment;
 
public record TossConfirmRequest(
        String qrToken,      // 우리 쪽 결제 식별자
        String paymentKey,   // 토스가 발급한 결제 키
        String orderId,      // 토스에 보냈던 orderId (qrToken과 같아야 함)
        Integer amount
) {
}
 
