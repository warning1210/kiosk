package com.kiosk.hq.delivery.controller;

import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.delivery.dto.DeliveryDispatchRequest;
import com.kiosk.hq.delivery.dto.DeliveryResponse;
import com.kiosk.hq.delivery.dto.DeliverySummaryResponse;
import com.kiosk.hq.delivery.service.HqDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본사 관리자가 쓰는 배송 관리 API.
 *
 * <p>재고 신청 API와 분리되어 있다. 승인까지는 재고 신청 화면에서, 그 뒤의 출고·배송은 여기서 다룬다.
 */
@RestController
@RequestMapping("/api/hq/deliveries")
@RequiredArgsConstructor
public class HqDeliveryController {

    private final HqDeliveryService hqDeliveryService;
    private final HqAccessService hqAccessService;

    /** 배송 목록 (HQ-013). status를 빼면 배송 관련 단계 전체를 본다. */
    @GetMapping
    public Page<DeliveryResponse> getDeliveries(
            @RequestParam(required = false) StockRequestStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqDeliveryService.getDeliveries(status, branchId, keyword, pageable);
    }

    /** 상태별 건수 요약 (지연 포함). */
    @GetMapping("/summary")
    public DeliverySummaryResponse getSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqDeliveryService.getSummary();
    }

    /** 출고 처리 (HQ-013). 배송번호는 자동 발급되고, 배송담당자만 입력받는다. */
    @PatchMapping("/{stockRequestId}/dispatch")
    public DeliveryResponse dispatch(
            @PathVariable Long stockRequestId,
            @Valid @RequestBody DeliveryDispatchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqDeliveryService.dispatch(stockRequestId, request);
    }
}
