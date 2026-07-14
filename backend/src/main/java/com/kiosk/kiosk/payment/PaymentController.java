package com.kiosk.kiosk.payment;
 
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
 
    private final PaymentService paymentService;
 
    @PostMapping("/qr")
    public PaymentQrResponse createQr(@RequestBody PaymentQrRequest request) {
        return paymentService.createQr(request.orderId());
    }
 
    @GetMapping("/{qrToken}")
    public PaymentStatusResponse getStatus(@PathVariable String qrToken) {
        return paymentService.getStatus(qrToken);
    }
 
    // QR 스캔 시 열리는 결제 페이지가 초기 로드 시 호출 (토스 SDK 초기화에 필요한 정보 제공)
    @GetMapping("/{qrToken}/checkout")
    public PaymentCheckoutResponse getCheckoutInfo(@PathVariable String qrToken) {
        return paymentService.getCheckoutInfo(qrToken);
    }
 
    // 토스 successUrl에서 프론트가 호출하는 실제 결제 승인 (qrToken은 request body 안에 포함)
    @PostMapping("/toss/confirm")
    public PaymentStatusResponse confirmWithToss(@RequestBody TossConfirmRequest request) {
        return paymentService.confirmWithToss(request);
    }
}