package com.example.kiosksim.controller;

import com.example.kiosksim.dto.PaymentQrRequest;
import com.example.kiosksim.dto.PaymentQrResponse;
import com.example.kiosksim.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/qr")
    public ResponseEntity<PaymentQrResponse> generateQrCode(@Valid @RequestBody PaymentQrRequest request) {
        PaymentQrResponse response = paymentService.generateQrCode(request.orderId(), request.amount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPaymentWithPoints(
            @RequestParam Long orderId,
            @RequestParam Integer finalAmount,
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "0") Integer usedPoints) {
        PaymentService.PaymentConfirmResponse response = paymentService.confirmPaymentWithPoints(
                orderId, finalAmount, phoneNumber, usedPoints);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/confirm-old")
    public ResponseEntity<PaymentQrResponse> confirmPayment(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam String amount) {
        PaymentQrResponse response = paymentService.confirmPayment(paymentKey, orderId, amount);
        return ResponseEntity.ok(response);
    }
}
