package com.kiosk.hq.stockrequest;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.ActorGuard;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import com.kiosk.stockrequest.dto.StockRequestResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 본사 관리자가 지점 재고 신청을 검색하고 처리하는 업무 흐름을 담당한다.
 *
 * <p>모든 공개 메서드의 시작에서 본사 권한을 확인한다. 조회 메서드는 읽기 전용
 * 트랜잭션을 사용하고, 승인·반려·배송처럼 상태를 바꾸는 메서드만 쓰기 트랜잭션으로
 * 재정의한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class HqStockRequestService {

    /** 신청 검색, 상태별 집계, 잠금 조회를 담당하는 신청 저장소다. */
    private final StockRequestRepository stockRequestRepository;
    /** 신청별 품목 조회와 승인 수량 변경에 사용하는 품목 저장소다. */
    private final StockRequestItemRepository stockRequestItemRepository;

    /**
     * 생성자 주입으로 본사 업무에 필요한 저장소를 받는다.
     *
     * @param stockRequestRepository 재고 신청 저장소
     * @param stockRequestItemRepository 재고 신청 품목 저장소
     */
    public HqStockRequestService(
            StockRequestRepository stockRequestRepository,
            StockRequestItemRepository stockRequestItemRepository) {
        this.stockRequestRepository = stockRequestRepository;
        this.stockRequestItemRepository = stockRequestItemRepository;
    }

    /**
     * 본사 검색 조건을 저장소 형식으로 정리해 신청 목록을 조회한다.
     *
     * @param admin 조회를 요청한 현재 관리자
     * @param status 선택적인 신청 상태
     * @param branchId 선택적인 지점 기본키
     * @param from 선택적인 조회 시작 시각
     * @param to 선택적인 조회 종료 시각
     * @param keyword 선택적인 신청 번호·지점명·맛 이름 검색어
     * @param pageable 페이지 정보
     * @return 품목이 결합된 신청 응답 페이지
     */
    public Page<StockRequestResponse> getStockRequests(
            Admin admin,
            StockRequestStatus status,
            Long branchId,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            Pageable pageable) {
        // URL만 알고 있는 지점 계정이 본사 전체 신청을 조회하지 못하도록 가장 먼저 검사한다.
        ActorGuard.requireHqRole(admin);

        // 앞뒤 공백과 대소문자를 정리하고, 부분 검색을 위해 검색어 양쪽에 %를 붙인다.
        String keywordPattern = normalizeKeyword(keyword);
        Page<StockRequest> requests = stockRequestRepository.searchForHq(
                status, branchId, from, to, keywordPattern, pageable);
        // 신청과 품목을 공용 응답 DTO로 변환해 영속성 엔티티를 API 밖으로 노출하지 않는다.
        return mapToResponsePage(requests);
    }

    /**
     * 본사 화면의 요약 카드에 필요한 상태별 신청 건수를 계산한다.
     *
     * <p>여기서 {@code approvedCount}는 승인 순간만 뜻하지 않고, 승인 이후의
     * 준비·배송·수령 완료 상태까지 포함한 "승인 후 정상 처리 건"을 뜻한다.</p>
     *
     * @param admin 조회를 요청한 현재 관리자
     * @return 전체, 대기, 승인 후 정상 처리, 반려 건수
     */
    public StockRequestSummaryResponse getSummary(Admin admin) {
        ActorGuard.requireHqRole(admin);

        // DB의 COUNT 쿼리를 사용하므로 모든 신청 엔티티를 메모리로 불러오지 않는다.
        long total = stockRequestRepository.count();
        long pending = stockRequestRepository.countByRequestStatus(StockRequestStatus.PENDING);
        long rejected = stockRequestRepository.countByRequestStatus(StockRequestStatus.REJECTED);
        long approved = stockRequestRepository.countByRequestStatusNotIn(
                Set.of(StockRequestStatus.PENDING, StockRequestStatus.REJECTED, StockRequestStatus.CLOSED));

        return new StockRequestSummaryResponse(total, pending, approved, rejected);
    }

    /**
     * 대기 중인 신청을 승인하고 각 품목의 신청 수량을 승인 수량으로 확정한다.
     *
     * <p>신청과 품목 변경은 한 쓰기 트랜잭션으로 처리된다. 신청 행을 먼저 잠그므로
     * 동일한 신청에 대한 승인·반려 요청이 동시에 성공할 수 없다.</p>
     *
     * @param admin 승인하는 현재 본사 관리자
     * @param stockRequestId 승인할 신청 기본키
     * @return 승인된 신청과 품목 정보
     */
    @Transactional
    public StockRequestResponse approveStockRequest(Admin admin, Long stockRequestId) {
        ActorGuard.requireHqRole(admin);
        // 비관적 잠금으로 읽은 뒤 상태를 검사해야 검사와 변경 사이에 다른 처리가 끼어들지 않는다.
        StockRequest stockRequest = findRequestForUpdate(stockRequestId);
        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 승인할 수 있습니다");

        // 현재 정책은 부분 승인이 없으므로 각 품목의 신청 수량 전체를 승인한다.
        List<StockRequestItem> items = loadItems(stockRequestId);
        for (StockRequestItem item : items) {
            item.approveRequestedQuantity();
        }

        // 영속 상태 엔티티의 변경은 트랜잭션 커밋 시 더티 체킹으로 DB에 반영된다.
        stockRequest.approve(admin, LocalDateTime.now());
        return StockRequestResponse.from(stockRequest, items);
    }

    /**
     * 대기 중인 신청을 반려하고 처리 관리자, 사유, 처리 시각을 기록한다.
     *
     * @param admin 반려하는 현재 본사 관리자
     * @param stockRequestId 반려할 신청 기본키
     * @param request 입력 검증을 통과한 반려 사유
     * @return 반려된 신청과 기존 품목 정보
     */
    @Transactional
    public StockRequestResponse rejectStockRequest(Admin admin, Long stockRequestId, RejectRequest request) {
        ActorGuard.requireHqRole(admin);
        // 같은 신청의 동시 승인을 막기 위해 상태 확인 전 신청 행을 잠근다.
        StockRequest stockRequest = findRequestForUpdate(stockRequestId);
        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 반려할 수 있습니다");

        stockRequest.reject(admin, request.rejectionReason(), LocalDateTime.now());
        return StockRequestResponse.from(stockRequest, loadItems(stockRequestId));
    }

    /**
     * 준비 중인 신청에 운송 정보를 기록하고 배송 중 상태로 전환한다.
     *
     * @param admin 배송을 등록하는 현재 본사 관리자
     * @param stockRequestId 배송할 신청 기본키
     * @param request 운송장 번호와 배송 부가 정보
     * @return 배송 중으로 전환된 신청과 품목 정보
     */
    @Transactional
    public StockRequestResponse shipStockRequest(Admin admin, Long stockRequestId, ShipRequest request) {
        ActorGuard.requireHqRole(admin);
        // 중복 배송 등록을 방지하기 위해 잠금 상태에서 PREPARING 여부를 확인한다.
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

    /**
     * 사용자가 입력한 검색어를 대소문자 구분 없는 부분 검색 패턴으로 바꾼다.
     *
     * @param keyword 원본 검색어
     * @return 공백 검색이면 {@code null}, 값이 있으면 소문자 {@code %검색어%} 패턴
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /**
     * 신청을 비관적 쓰기 잠금으로 조회한다.
     *
     * <p>호출한 트랜잭션이 끝날 때까지 같은 행의 경쟁 상태 변경을 기다리게 하므로,
     * 이후 상태 검사와 변경이 하나의 원자적인 업무처럼 실행된다.</p>
     *
     * @param stockRequestId 조회할 신청 기본키
     * @return 잠금이 획득된 재고 신청
     */
    private StockRequest findRequestForUpdate(Long stockRequestId) {
        return stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
    }

    /**
     * 한 신청에 속한 모든 품목을 조회한다.
     *
     * @param stockRequestId 품목을 조회할 신청 기본키
     * @return 신청 품목 목록
     */
    private List<StockRequestItem> loadItems(Long stockRequestId) {
        return stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
    }

    /**
     * 현재 상태가 해당 동작을 시작할 수 있는 상태인지 확인한다.
     *
     * @param stockRequest 검사할 신청
     * @param expectedStatus 기대하는 현재 상태
     * @param message 상태 충돌 시 응답할 설명
     */
    private void requireStatus(StockRequest stockRequest, StockRequestStatus expectedStatus, String message) {
        if (stockRequest.getRequestStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /**
     * 신청 페이지의 품목을 일괄 조회해 공용 API 응답으로 변환한다.
     *
     * <p>페이지의 신청 기본키들을 한 번의 {@code IN} 조회에 사용하고 신청별로 그룹화한다.
     * 이렇게 하면 신청마다 품목 쿼리를 실행하는 N+1 조회를 피할 수 있다.</p>
     *
     * @param requests DB에서 조회한 신청 페이지
     * @return 품목 목록을 포함한 응답 페이지
     */
    private Page<StockRequestResponse> mapToResponsePage(Page<StockRequest> requests) {
        // 현재 페이지에 포함된 신청의 기본키만 모은다.
        List<Long> requestIds = requests.getContent().stream()
                .map(StockRequest::getStockRequestId)
                .toList();

        // 빈 페이지에서는 불필요한 품목 쿼리를 실행하지 않는다.
        Map<Long, List<StockRequestItem>> itemsByRequestId = requestIds.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));

        // Page.map을 사용해 페이지 번호·전체 건수는 유지하고 내용만 DTO로 바꾼다.
        return requests.map(stockRequest -> StockRequestResponse.from(
                stockRequest,
                itemsByRequestId.getOrDefault(stockRequest.getStockRequestId(), List.of())));
    }
}
