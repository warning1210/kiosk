package com.kiosk.branch.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.CurrentAdmin;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BranchStockRequestController {

    private final BranchStockRequestService branchStockRequestService;

    public BranchStockRequestController(BranchStockRequestService branchStockRequestService) {
        this.branchStockRequestService = branchStockRequestService;
    }

    @PostMapping("/api/branch/stock-requests")
    public StockRequestResponse create(@CurrentAdmin Admin admin,
                                       @Valid @RequestBody StockRequestCreateRequest request) {
        return branchStockRequestService.create(admin, request);
    }

    @GetMapping("/api/branch/stock-requests")
    public Page<StockRequestResponse> list(@CurrentAdmin Admin admin,
                                           @RequestParam(required = false) StockRequestStatus status,
                                           Pageable pageable) {
        return branchStockRequestService.list(admin, status, pageable);
    }

    @PatchMapping("/api/branch/stock-requests/{id}/cancel")
    public ResponseEntity<Void> cancel(@CurrentAdmin Admin admin, @PathVariable Long id) {
        branchStockRequestService.cancel(admin, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/branch/stock-requests/{id}/confirm-receipt")
    public StockRequestResponse confirmReceipt(@CurrentAdmin Admin admin, @PathVariable Long id) {
        return branchStockRequestService.confirmReceipt(admin, id);
    }
}
