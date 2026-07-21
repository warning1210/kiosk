package com.kiosk.hq.stockrequest.controller;

import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import com.kiosk.hq.stockrequest.service.HqStockRequestService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본사 관리자가 쓰는 재고 신청 관리 API.
 *
 * <p>다른 본사 컨트롤러와 동일하게 {@link HqAccessService}로 본사 권한을 확인한 뒤 서비스를 호출한다.
 */
@RestController
@RequestMapping("/api/hq/stock-requests")
@RequiredArgsConstructor
public class HqStockRequestController {

    private final HqStockRequestService hqStockRequestService;
    private final HqAccessService hqAccessService;

    /** 전 지점 신청 검색 (HQ-001). 모든 조건은 선택이라 필요한 것만 보내면 된다. */
    @GetMapping
    public Page<StockRequestResponse> getStockRequests(
            @RequestParam(required = false) StockRequestStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqStockRequestService.getStockRequests(admin, status, branchId, from, to, keyword, pageable);
    }

    /** 상태별 신청 건수 요약 (HQ-004). */
    @GetMapping("/summary")
    public StockRequestSummaryResponse getSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqStockRequestService.getSummary(admin);
    }

    /** 신청 승인 (HQ-002). */
    @PatchMapping("/{stockRequestId}/approve")
    public StockRequestResponse approveStockRequest(
            @PathVariable Long stockRequestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqStockRequestService.approveStockRequest(admin, stockRequestId);
    }

    /** 신청 반려 (HQ-003). 사유 필수. */
    @PatchMapping("/{stockRequestId}/reject")
    public StockRequestResponse rejectStockRequest(
            @PathVariable Long stockRequestId,
            @Valid @RequestBody RejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqStockRequestService.rejectStockRequest(admin, stockRequestId, request);
    }

    // 승인 뒤 출고(배송)는 별도 배송 관리 API로 분리되었다: PATCH /api/hq/deliveries/{id}/dispatch
}
