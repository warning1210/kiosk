package com.kiosk.kiosk.payment.controller;

import com.kiosk.kiosk.payment.dto.PaymentQrRequest;
import com.kiosk.kiosk.payment.dto.PaymentQrResponse;
import com.kiosk.kiosk.payment.dto.PaymentStatusResponse;
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

    @PostMapping("/qr")
    public PaymentQrResponse createQr(@RequestBody PaymentQrRequest request) {
        return paymentService.createQr(request.orderId());
    }

    @GetMapping("/{qrToken}")
    public PaymentStatusResponse getStatus(@PathVariable String qrToken) {
        return paymentService.getStatus(qrToken);
    }

    @PostMapping("/{qrToken}/confirm")
    public PaymentStatusResponse confirm(@PathVariable String qrToken) {
        return paymentService.confirm(qrToken);
    }
}
