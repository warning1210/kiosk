package com.kiosk.kiosk.payment.controller;

import com.kiosk.kiosk.payment.dto.PaymentCheckoutResponse;
import com.kiosk.kiosk.payment.dto.CashPaymentRequest;
import com.kiosk.kiosk.payment.dto.CashPaymentResponse;
import com.kiosk.kiosk.payment.dto.PaymentQrRequest;
import com.kiosk.kiosk.payment.dto.PaymentQrResponse;
import com.kiosk.kiosk.payment.dto.PaymentStatusResponse;
import com.kiosk.kiosk.payment.dto.TossConfirmRequest;
import com.kiosk.kiosk.payment.service.PaymentService;
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

    /**
     * 키오스크 현금 선택 전용 API.
     * 실제 결제 승인이 아니라 카운터에 가져갈 주문서를 발급할 수 있도록 현금 대기 주문을 등록한다.
     */
    @PostMapping("/cash")
    public CashPaymentResponse createCash(@RequestBody CashPaymentRequest request) {
        return paymentService.createCash(request.orderId());
    }

    @PostMapping("/qr")
    public PaymentQrResponse createQr(@RequestBody PaymentQrRequest request) {
        return paymentService.createQr(request.orderId());
    }

    @GetMapping("/{qrToken}")
    public PaymentStatusResponse getStatus(@PathVariable String qrToken) {
        return paymentService.getStatus(qrToken);
    }

    @GetMapping("/{qrToken}/checkout")
    public PaymentCheckoutResponse getCheckoutInfo(@PathVariable String qrToken) {
        return paymentService.getCheckoutInfo(qrToken);
    }

    @PostMapping("/toss/confirm")
    public PaymentStatusResponse confirmWithToss(@RequestBody TossConfirmRequest request) {
        return paymentService.confirmWithToss(request);
    }
}
