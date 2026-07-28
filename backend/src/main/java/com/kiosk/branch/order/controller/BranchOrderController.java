package com.kiosk.branch.order.controller;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.BranchOrderDetailResponse;
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

    /**
     * 현금 수령 확인 버튼 전용 API. 키오스크 주문 생성과 실제 현금 수령 시점을 분리한다.
     */
    @PostMapping("/{orderId}/confirm-cash")
    public ResponseEntity<Void> confirmCash(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        branchOrderService.confirmCashPayment(branchId, orderId);
        return ResponseEntity.noContent().build();
    }
    private final BranchAccessService branchAccessService;

    @GetMapping
    public ResponseEntity<List<BranchOrderListResponse>> getBranchOrders(
            @RequestParam(required = false) java.time.LocalDate date,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        List<BranchOrderListResponse> orders = branchOrderService.getBranchOrders(branchId, date, activeOnly);
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

    @GetMapping("/{orderId}")
    public ResponseEntity<BranchOrderDetailResponse> getBranchOrderDetail(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        BranchOrderDetailResponse response = branchOrderService.getBranchOrderDetail(branchId, orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dates")
    public ResponseEntity<List<String>> getAvailableOrderDates(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        List<String> dates = branchOrderService.getAvailableOrderDates(branchId);
        return ResponseEntity.ok(dates);
    }
}
