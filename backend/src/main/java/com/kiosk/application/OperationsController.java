package com.kiosk.application;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OperationsController {
    private final OrderWorkflowService service;
    private final BranchAccessService branchAccess;
    public OperationsController(OrderWorkflowService service, BranchAccessService branchAccess) { this.service = service; this.branchAccess = branchAccess; }

    @PostMapping("/kiosk/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderWorkflowService.CreateOrderResponse create(@RequestBody OrderWorkflowService.CreateOrderRequest request) {
        return service.createOrder(request);
    }
    @GetMapping("/branch/orders")
    public List<OrderWorkflowService.OrderResponse> orders(@RequestHeader(value="Authorization",required=false) String authorization) {
        return service.getOrders(branchAccess.requireBranchId(authorization));
    }
    @PatchMapping("/branch/orders/{orderId}/status")
    public OrderWorkflowService.OrderResponse status(@PathVariable Long orderId, @RequestBody Map<String,String> body,
            @RequestHeader(value="Authorization",required=false) String authorization) {
        return service.updateStatus(orderId, body.get("status"), branchAccess.requireBranchId(authorization));
    }
    @GetMapping("/branch/inventory")
    public List<OrderWorkflowService.InventoryResponse> inventory(@RequestHeader(value="Authorization",required=false) String authorization) {
        return service.getInventory(branchAccess.requireBranchId(authorization));
    }
    @PatchMapping("/branch/inventory/{inventoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjust(@PathVariable Long inventoryId, @RequestBody Map<String,String> body,
            @RequestHeader(value="Authorization",required=false) String authorization) {
        service.adjustInventory(inventoryId, body.getOrDefault("type", "OUT"), Integer.parseInt(body.getOrDefault("grams", "0")), branchAccess.requireBranchId(authorization));
    }
    @PostMapping("/branch/inventory/{inventoryId}/receive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(@PathVariable Long inventoryId,@RequestHeader(value="Authorization",required=false) String authorization) {
        service.receiveInventory(inventoryId, branchAccess.requireBranchId(authorization));
    }
}
