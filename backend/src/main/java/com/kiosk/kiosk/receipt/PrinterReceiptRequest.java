package com.kiosk.kiosk.receipt;

/**
 * 강사님 프린터 서비스(POST :8888/receipt)가 받는 JSON 형식.
 * 기존 4개 필드는 프린터 서비스와의 호환을 위해 유지한다.
 * orderLabel/printBarcode는 새 프린터 서비스가 현금 주문서 제목과 바코드 출력 여부를
 * 구분할 수 있도록 추가한 값이다.
 *
 * 예: {"orderNo":"001","orderItem":"파인트 외 2건","price":"13,700원","orderDate":"2026년07월14일"}
 */
public record PrinterReceiptRequest(
        String orderNo,
        String orderItem,
        String price,
        String orderDate,
        String orderLabel,
        boolean printBarcode
) {
}
