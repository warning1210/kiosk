package com.kiosk.hq.stockrequest.service;

import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 본사가 전 지점의 재고 신청을 조회하고 승인·반려·배송 등록하는 업무를 담당한다
 * (HQ-001, HQ-002, HQ-003, HQ-013).
 *
 * <p>상태 전이는 PENDING -> (승인) PREPARING -> (배송등록) SHIPPING -> (지점 수령확인) DELIVERED 순서다.
 * 두 사람이 같은 신청을 동시에 처리하면 검사와 변경 사이에 값이 바뀔 수 있어서,
 * 상태를 바꾸는 메서드는 모두 행을 잠근 뒤 상태를 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqStockRequestService {

    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;

    /** 조건을 조합해 전 지점 신청을 검색한다 (HQ-001). 안 보낸 조건은 무시된다. */
    public Page<StockRequestResponse> getStockRequests(
            Admin admin,
            StockRequestStatus status,
            Long branchId,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            Pageable pageable) {
        requireHqRole(admin);
        Page<StockRequest> requests = stockRequestRepository.searchForHq(
                status, branchId, from, to, normalizeKeyword(keyword), pageable);
        return mapToResponsePage(requests);
    }

    /** 대시보드 상단 카드용 상태별 건수 (HQ-004). COUNT 쿼리라 전체를 메모리로 읽지 않는다. */
    public StockRequestSummaryResponse getSummary(Admin admin) {
        requireHqRole(admin);
        long total = stockRequestRepository.count();
        long pending = stockRequestRepository.countByRequestStatus(StockRequestStatus.PENDING);
        long rejected = stockRequestRepository.countByRequestStatus(StockRequestStatus.REJECTED);
        // "승인됨"은 대기·반려·종료를 뺀 나머지, 즉 실제로 진행 중이거나 끝난 건들을 뜻한다.
        long approved = stockRequestRepository.countByRequestStatusNotIn(
                Set.of(StockRequestStatus.PENDING, StockRequestStatus.REJECTED, StockRequestStatus.CLOSED));
        return new StockRequestSummaryResponse(total, pending, approved, rejected);
    }

    /** 신청 승인 (HQ-002). 지금은 부분 승인이 없어서 신청 수량을 그대로 승인 수량으로 확정한다. */
    @Transactional
    public StockRequestResponse approveStockRequest(Admin admin, Long stockRequestId) {
        requireHqRole(admin);
        StockRequest stockRequest = findRequestForUpdate(stockRequestId);
        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 승인할 수 있습니다");

        List<StockRequestItem> items = loadItems(stockRequestId);
        for (StockRequestItem item : items) {
            item.approve(item.getRequestedQuantity());
        }
        stockRequest.approve(admin, LocalDateTime.now());
        return StockRequestResponse.from(stockRequest, items);
    }

    /** 신청 반려 (HQ-003). 사유는 DTO 검증에서 이미 비어 있지 않음이 보장된다. */
    @Transactional
    public StockRequestResponse rejectStockRequest(Admin admin, Long stockRequestId, RejectRequest request) {
        requireHqRole(admin);
        StockRequest stockRequest = findRequestForUpdate(stockRequestId);
        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 반려할 수 있습니다");

        stockRequest.reject(admin, request.rejectionReason(), LocalDateTime.now());
        return StockRequestResponse.from(stockRequest, loadItems(stockRequestId));
    }

    /** 배송 등록 (HQ-013). 승인되어 출고 준비 상태인 건만 배송으로 넘길 수 있다. */
    @Transactional
    public StockRequestResponse shipStockRequest(Admin admin, Long stockRequestId, ShipRequest request) {
        requireHqRole(admin);
        StockRequest stockRequest = findRequestForUpdate(stockRequestId);
        requireStatus(stockRequest, StockRequestStatus.PREPARING, "배송 준비중인 신청만 배송 등록할 수 있습니다");

        stockRequest.startShipping(
                request.trackingNumber(),
                request.courierName(),
                request.driverName(),
                request.estimatedArrivalAt(),
                LocalDateTime.now());
        return StockRequestResponse.from(stockRequest, loadItems(stockRequestId));
    }

    // --- 보조 메서드 ---

    /**
     * 본사 권한인지 확인한다.
     *
     * <p>컨트롤러에서 {@code HqAccessService}가 이미 확인하지만, 서비스만 따로 호출되는 경우에도
     * 지점 계정이 전 지점 데이터를 보지 못하도록 한 번 더 막는다.
     */
    private void requireHqRole(Admin admin) {
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
    }

    /** 부분 검색이 되도록 소문자로 바꾸고 양쪽에 %를 붙인다. 빈 검색어는 조건에서 제외되도록 null로 만든다. */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private StockRequest findRequestForUpdate(Long stockRequestId) {
        return stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
    }

    private List<StockRequestItem> loadItems(Long stockRequestId) {
        return stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
    }

    private void requireStatus(StockRequest stockRequest, StockRequestStatus expectedStatus, String message) {
        if (stockRequest.getRequestStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /** 목록에 품목을 붙인다. 신청마다 따로 조회하면 N+1이 나서 한 번에 모아 온다. */
    private Page<StockRequestResponse> mapToResponsePage(Page<StockRequest> requests) {
        List<Long> requestIds = requests.getContent().stream().map(StockRequest::getStockRequestId).toList();
        Map<Long, List<StockRequestItem>> itemsByRequestId = requestIds.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));
        return requests.map(stockRequest -> StockRequestResponse.from(
                stockRequest, itemsByRequestId.getOrDefault(stockRequest.getStockRequestId(), List.of())));
    }
}
