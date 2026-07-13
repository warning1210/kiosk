package com.kiosk.hq.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.CurrentAdmin;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HqStockRequestController {

    private final HqStockRequestService hqStockRequestService;

    public HqStockRequestController(HqStockRequestService hqStockRequestService) {
        this.hqStockRequestService = hqStockRequestService;
    }

    @GetMapping("/api/hq/stock-requests")
    public Page<StockRequestResponse> list(
            @CurrentAdmin Admin admin,
            @RequestParam(required = false) StockRequestStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return hqStockRequestService.list(admin, status, branchId, from, to, keyword, pageable);
    }

    @GetMapping("/api/hq/stock-requests/summary")
    public StockRequestSummaryResponse summary(@CurrentAdmin Admin admin) {
        return hqStockRequestService.summary(admin);
    }

    @PatchMapping("/api/hq/stock-requests/{id}/approve")
    public StockRequestResponse approve(@CurrentAdmin Admin admin, @PathVariable Long id) {
        return hqStockRequestService.approve(admin, id);
    }

    @PatchMapping("/api/hq/stock-requests/{id}/reject")
    public StockRequestResponse reject(@CurrentAdmin Admin admin, @PathVariable Long id,
                                       @Valid @RequestBody RejectRequest request) {
        return hqStockRequestService.reject(admin, id, request);
    }

    @PatchMapping("/api/hq/stock-requests/{id}/ship")
    public StockRequestResponse ship(@CurrentAdmin Admin admin, @PathVariable Long id,
                                     @Valid @RequestBody ShipRequest request) {
        return hqStockRequestService.ship(admin, id, request);
    }
}
