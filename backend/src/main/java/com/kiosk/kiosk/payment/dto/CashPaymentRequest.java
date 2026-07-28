package com.kiosk.kiosk.payment.dto;

/**
 * 키오스크에서 현금 주문서를 만들 때 전달하는 값.
 * 상품/금액은 이미 생성된 주문에서 다시 조회하므로 orderId만 받는다.
 */
public record CashPaymentRequest(Long orderId) {
}
