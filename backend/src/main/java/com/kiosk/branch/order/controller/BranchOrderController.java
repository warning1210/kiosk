package com.kiosk.branch.order.controller;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
import com.kiosk.branch.order.service.BranchOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branch/{branchId}/orders")
public class BranchOrderController {

    private final BranchOrderService branchOrderService;

    @GetMapping
    public ResponseEntity<List<BranchOrderListResponse>> getBranchOrders(@PathVariable Long branchId) {
        List<BranchOrderListResponse> orders = branchOrderService.getBranchOrders(branchId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long branchId, 
            @PathVariable Long orderId, 
            @RequestBody OrderStatusUpdateRequest request) {
        
        branchOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok().build();
    }
}
