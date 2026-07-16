package com.kiosk.branch.order.controller;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
import com.kiosk.branch.order.service.BranchOrderService;
import com.kiosk.global.security.BranchAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branch/orders")
public class BranchOrderController {

    private final BranchOrderService branchOrderService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public ResponseEntity<List<BranchOrderListResponse>> getBranchOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        List<BranchOrderListResponse> orders = branchOrderService.getBranchOrders(branchId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        branchOrderService.updateOrderStatus(branchId, orderId, request);
        return ResponseEntity.ok().build();
    }
}
