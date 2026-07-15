package com.kiosk.kiosk.receipt;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 영수증 관련 API.
 *   GET  /api/orders/{orderId}/receipt  → 화면에 보여줄 영수증 데이터
 *   POST /api/orders/{orderId}/print    → 강사님 프린터로 실제 출력 요청
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/{orderId}/receipt")
    public ReceiptResponse getReceipt(@PathVariable Long orderId) {
        return receiptService.getReceipt(orderId);
    }

    @PostMapping("/{orderId}/print")
    public Map<String, Boolean> printReceipt(@PathVariable Long orderId) {
        // printed=true 면 프린터 출력 성공, false 면 프린터 오프라인(주문은 정상)
        return Map.of("printed", receiptService.printReceipt(orderId));
    }
}
