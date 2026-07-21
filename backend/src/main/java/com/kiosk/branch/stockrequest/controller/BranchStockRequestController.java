package com.kiosk.branch.stockrequest.controller;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.branch.stockrequest.service.BranchStockRequestService;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.BranchAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지점 관리자가 쓰는 재고 신청 API.
 *
 * <p>다른 지점 백오피스 컨트롤러와 같은 방식으로, 어느 지점인지는 URL이 아니라
 * {@link BranchAccessService}가 Authorization 헤더에서 판별한다. 그래서 주소를 바꿔서
 * 남의 지점 신청을 조회하는 것이 불가능하다.
 */
@RestController
@RequestMapping("/api/branch/stock-requests")
@RequiredArgsConstructor
public class BranchStockRequestController {

    private final BranchStockRequestService branchStockRequestService;
    private final BranchAccessService branchAccessService;

    /** 새 재고 신청 등록 (BR-001). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockRequestResponse createStockRequest(
            @Valid @RequestBody StockRequestCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return branchStockRequestService.createStockRequest(admin, request);
    }

    /** 내 지점 신청 목록 조회 (BR-002). status를 빼면 전체 상태를 본다. */
    @GetMapping
    public Page<StockRequestResponse> getStockRequests(
            @RequestParam(required = false) StockRequestStatus status,
            Pageable pageable,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return branchStockRequestService.getStockRequests(admin, status, pageable);
    }

    /** 아직 본사가 처리하지 않은 신청 취소. */
    @PatchMapping("/{stockRequestId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStockRequest(
            @PathVariable Long stockRequestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        branchStockRequestService.cancelStockRequest(admin, stockRequestId);
    }

    /** 배송 온 물건 수령 확인 (BR-010). 재고 수량 반영은 재고 화면의 입고 처리에서 한다. */
    @PatchMapping("/{stockRequestId}/confirm-receipt")
    public StockRequestResponse confirmStockReceipt(
            @PathVariable Long stockRequestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return branchStockRequestService.confirmStockReceipt(admin, stockRequestId);
    }
}
