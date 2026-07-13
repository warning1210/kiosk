package com.kiosk.kiosk.order.controller;

import com.kiosk.kiosk.order.dto.OrderCheckoutRequest;
import com.kiosk.kiosk.order.dto.OrderCheckoutResponse;
import com.kiosk.kiosk.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public OrderCheckoutResponse checkout(@RequestBody OrderCheckoutRequest request) {
        return orderService.checkout(request);
    }
}
