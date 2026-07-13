package com.kiosk.hq.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.exception.BusinessException;
import com.kiosk.global.exception.ErrorCode;
import com.kiosk.global.security.ActorGuard;
import com.kiosk.hq.stockrequest.dto.ApproveItemRequest;
import com.kiosk.hq.stockrequest.dto.ApproveRequest;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HqStockRequestService {

    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;

    public HqStockRequestService(StockRequestRepository stockRequestRepository,
                                  StockRequestItemRepository stockRequestItemRepository) {
        this.stockRequestRepository = stockRequestRepository;
        this.stockRequestItemRepository = stockRequestItemRepository;
    }

    public Page<StockRequestResponse> list(Admin admin, StockRequestStatus status, Long branchId,
                                            LocalDateTime from, LocalDateTime to, String keyword, Pageable pageable) {
        ActorGuard.requireHqRole(admin);
        Page<StockRequest> page = stockRequestRepository.findAll(
                StockRequestSpecifications.filter(status, branchId, from, to, keyword), pageable);
        return toResponsePage(page);
    }

    public StockRequestSummaryResponse summary(Admin admin) {
        ActorGuard.requireHqRole(admin);
        long total = stockRequestRepository.count();
        long pending = stockRequestRepository.countByRequestStatus(StockRequestStatus.PENDING);
        long rejected = stockRequestRepository.countByRequestStatus(StockRequestStatus.REJECTED);
        long approved = stockRequestRepository.countByRequestStatusNotIn(
                Set.of(StockRequestStatus.PENDING, StockRequestStatus.REJECTED, StockRequestStatus.CLOSED));
        return new StockRequestSummaryResponse(total, pending, approved, rejected);
    }

    @Transactional
    public StockRequestResponse approve(Admin admin, Long stockRequestId, ApproveRequest request) {
        ActorGuard.requireHqRole(admin);
        StockRequest existing = loadForUpdate(stockRequestId);
        if (existing.getRequestStatus() != StockRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "대기중인 신청만 승인할 수 있습니다");
        }

        List<StockRequestItem> items = stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        Map<Long, Integer> overridesByFlavorId = request.itemOverrides() == null ? Map.of()
                : request.itemOverrides().stream()
                        .collect(Collectors.toMap(ApproveItemRequest::flavorId, ApproveItemRequest::approvedQuantity));
        for (StockRequestItem item : items) {
            Integer override = overridesByFlavorId.get(item.getFlavor().getFlavorId());
            item.setApprovedQuantity(override != null ? override : item.getRequestedQuantity());
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setRequestStatus(StockRequestStatus.PREPARING);
        existing.setProcessedAdmin(admin);
        existing.setProcessedAt(now);

        return StockRequestResponse.from(existing, items);
    }

    @Transactional
    public StockRequestResponse reject(Admin admin, Long stockRequestId, RejectRequest request) {
        ActorGuard.requireHqRole(admin);
        StockRequest existing = loadForUpdate(stockRequestId);
        if (existing.getRequestStatus() != StockRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "대기중인 신청만 반려할 수 있습니다");
        }

        existing.setRequestStatus(StockRequestStatus.REJECTED);
        existing.setRejectionReason(request.rejectionReason());
        existing.setProcessedAdmin(admin);
        existing.setProcessedAt(LocalDateTime.now());

        List<StockRequestItem> items = stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        return StockRequestResponse.from(existing, items);
    }

    @Transactional
    public StockRequestResponse ship(Admin admin, Long stockRequestId, ShipRequest request) {
        ActorGuard.requireHqRole(admin);
        StockRequest existing = loadForUpdate(stockRequestId);
        if (existing.getRequestStatus() != StockRequestStatus.PREPARING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "배송 준비중인 신청만 배송 등록할 수 있습니다");
        }

        existing.setRequestStatus(StockRequestStatus.SHIPPING);
        existing.setTrackingNumber(request.trackingNumber());
        existing.setCourierName(request.courierName());
        existing.setDriverName(request.driverName());
        existing.setEstimatedArrivalAt(request.estimatedArrivalAt());
        existing.setShippedAt(LocalDateTime.now());

        List<StockRequestItem> items = stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        return StockRequestResponse.from(existing, items);
    }

    private StockRequest loadForUpdate(Long stockRequestId) {
        return stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
    }

    private Page<StockRequestResponse> toResponsePage(Page<StockRequest> page) {
        List<Long> ids = page.getContent().stream().map(StockRequest::getStockRequestId).toList();
        Map<Long, List<StockRequestItem>> itemsByRequestId = ids.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(ids).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));
        return page.map(sr -> StockRequestResponse.from(sr, itemsByRequestId.getOrDefault(sr.getStockRequestId(), List.of())));
    }
}
