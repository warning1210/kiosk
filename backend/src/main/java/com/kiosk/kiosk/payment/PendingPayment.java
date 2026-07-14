package com.kiosk.kiosk.payment;
 
import java.time.LocalDateTime;
 
/**
 * 결제가 완료되기 전까지는 DB에 저장하지 않고 메모리에만 들고 있는 임시 결제 세션.
 * 토스 승인이 성공하는 순간에만 실제 Payment 엔티티로 변환되어 저장된다.
 */
public record PendingPayment(
        Long orderId,
        String qrToken,
        Integer amount,
        LocalDateTime expiresAt
) {
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
 