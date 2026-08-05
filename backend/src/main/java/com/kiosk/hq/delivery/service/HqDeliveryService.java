package com.kiosk.hq.delivery.service;

import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.hq.delivery.dto.DeliveryDispatchRequest;
import com.kiosk.hq.delivery.dto.DeliveryResponse;
import com.kiosk.hq.delivery.dto.DeliverySummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * 본사의 배송(출고) 관리를 담당한다 (HQ-013·015).
 *
 * <p>재고 신청이 승인되면(PREPARING) 이 화면으로 넘어와 본사가 출고 처리를 한다. 택배사에 맡기는
 * 구조가 아니라 본사가 직접 배송하므로, 출고할 때 배송번호는 서버가 자동 발급하고 배송담당자만
 * 본사 담당자가 입력한다. 배송이 시작되면(SHIPPING) 도착 예정 시각이 지난 건을 "지연"으로 본다.
 * 실제 배송 완료(DELIVERED) 처리는 본사가 아니라 지점이 물품 수령을 확인할 때 이뤄진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqDeliveryService {

    // 배송 관리 화면에서 다루는 단계. 대기(PENDING)나 반려(REJECTED)는 여기서 보지 않는다.
    private static final Set<StockRequestStatus> DELIVERY_STATUSES = Set.of(
            StockRequestStatus.PREPARING, StockRequestStatus.SHIPPING, StockRequestStatus.DELIVERED);

    private static final DateTimeFormatter SHIPMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    // 도착 예정 시각을 따로 안 주면 이만큼 뒤로 잡는다 (본사 물류 기준 대략적인 값).
    private static final int DEFAULT_ARRIVAL_DAYS = 2;

    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;

    /** 배송 목록 조회 (HQ-013). status가 null이면 배송 관련 단계 전체를 본다. */
    public Page<DeliveryResponse> getDeliveries(StockRequestStatus status, Long branchId, String keyword,
            Pageable pageable) {
        Page<StockRequest> requests = stockRequestRepository.searchDeliveries(
                DELIVERY_STATUSES, status, branchId, normalizeKeyword(keyword), pageable);
        LocalDateTime now = LocalDateTime.now();
        return mapToResponsePage(requests, now);
    }

    /** 상단 카드용 상태별 건수 (지연 포함). */
    public DeliverySummaryResponse getSummary() {
        long preparing = stockRequestRepository.countByRequestStatus(StockRequestStatus.PREPARING);
        long shipping = stockRequestRepository.countByRequestStatus(StockRequestStatus.SHIPPING);
        long delivered = stockRequestRepository.countByRequestStatus(StockRequestStatus.DELIVERED);
        long delayed = stockRequestRepository.countByRequestStatusAndEstimatedArrivalAtBefore(
                StockRequestStatus.SHIPPING, LocalDateTime.now());
        return new DeliverySummaryResponse(preparing, shipping, delivered, delayed);
    }

    /**
     * 출고 처리 (HQ-013). 승인되어 출고 준비(PREPARING) 상태인 건만 배송으로 넘길 수 있다.
     *
     * <p>배송번호는 여기서 자동 발급하고, 배송담당자는 요청으로 받은 값을 쓴다. 도착 예정 시각을
     * 안 주면 기본값을 넣는다.
     */
    @Transactional
    public DeliveryResponse dispatch(Long stockRequestId, DeliveryDispatchRequest request) {
        StockRequest stockRequest = stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
        if (stockRequest.getRequestStatus() != StockRequestStatus.PREPARING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "출고 준비중인 신청만 출고 처리할 수 있습니다");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estimatedArrival = request.estimatedArrivalAt() != null
                ? request.estimatedArrivalAt()
                : now.plusDays(DEFAULT_ARRIVAL_DAYS);

        stockRequest.dispatch(generateShipmentNumber(stockRequest), request.driverName(), estimatedArrival, now);
        stockRequestRepository.update(stockRequest);

        List<StockRequestItem> items = stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        return DeliveryResponse.from(stockRequest, items, now);
    }

    // --- 보조 메서드 ---

    /** SHIP-날짜-신청PK 형태. 신청 PK가 들어가 같은 날 여러 건도 번호가 겹치지 않는다. */
    private String generateShipmentNumber(StockRequest stockRequest) {
        return "SHIP-" + LocalDate.now().format(SHIPMENT_DATE_FORMAT) + "-" + stockRequest.getStockRequestId();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /** 목록에 품목을 붙인다. 신청마다 따로 조회하면 N+1이 나서 한 번에 모아 온다. */
    private Page<DeliveryResponse> mapToResponsePage(Page<StockRequest> requests, LocalDateTime now) {
        List<Long> requestIds = requests.getContent().stream().map(StockRequest::getStockRequestId).toList();
        Map<Long, List<StockRequestItem>> itemsByRequestId = requestIds.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));
        return requests.map(stockRequest -> DeliveryResponse.from(
                stockRequest, itemsByRequestId.getOrDefault(stockRequest.getStockRequestId(), List.of()), now));
    }
}
