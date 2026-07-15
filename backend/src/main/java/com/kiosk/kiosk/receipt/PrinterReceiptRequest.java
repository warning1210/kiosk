package com.kiosk.kiosk.receipt;

/**
 * 강사님 프린터 서비스(POST :8888/receipt)가 받는 JSON 형식.
 * 필드 이름(orderNo/orderItem/price/orderDate)은 프린터 서비스가 정해둔 것이라
 * 절대 바꾸면 안 된다. 전부 문자열이다.
 *
 * 예: {"orderNo":"001","orderItem":"파인트 외 2건","price":"13,700원","orderDate":"2026년07월14일"}
 */
public record PrinterReceiptRequest(
        String orderNo,
        String orderItem,
        String price,
        String orderDate
) {
}
