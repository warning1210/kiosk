package com.kiosk.kiosk.order.controller;

import com.kiosk.kiosk.order.dto.OrderCreateRequest;
import com.kiosk.kiosk.order.service.KioskOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiosk/orders")
public class KioskOrderController {

    private final KioskOrderService kioskOrderService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> createOrder(@RequestBody OrderCreateRequest request) {
        Long orderId = kioskOrderService.createOrder(request);
        return ResponseEntity.ok(Map.of("orderId", orderId));
    }
}
