package com.example.kiosksim.controller;

import com.example.kiosksim.dto.OrderDraftRequest;
import com.example.kiosksim.dto.OrderResponse;
import com.example.kiosksim.dto.TableSnapshotResponse;
import com.example.kiosksim.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@Valid @RequestBody OrderDraftRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping
    public List<OrderResponse> orders() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse order(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping("/tables")
    public TableSnapshotResponse tables() {
        return orderService.tableSnapshot();
    }
}
