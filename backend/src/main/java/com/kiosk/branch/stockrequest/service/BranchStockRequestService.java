package com.kiosk.branch.stockrequest.service;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestItemRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.stockrequest.RequestType;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 지점이 본사에 재고를 신청하고, 신청 상태를 확인·취소하고, 수령을 확인하는 업무를 담당한다
 * (BR-001, BR-002, BR-003).
 *
 * <p>조회 범위는 항상 로그인한 관리자의 소속 지점으로 고정한다. 클라이언트가 branchId를 보내게 하면
 * 값을 바꿔 다른 지점 신청을 들여다볼 수 있으므로, branchId는 인증 정보에서만 얻는다.
 *
 * <p><b>수량 단위</b>: 신청 수량은 통(tub) 개수다. 실제 재고는 그램 단위로 관리되며,
 * 통 -> 그램 환산과 재고 증가는 지점 재고 화면의 입고 처리
 * ({@code BranchInventoryService.receiveInventory})가 담당한다. 여기서는 재고를 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchStockRequestService {

    private static final DateTimeFormatter REQUEST_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int REQUEST_NUMBER_MAX_LENGTH = 30;
    private static final String TEMPORARY_REQUEST_NUMBER_PREFIX = "TMP-";

    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;
    private final FlavorRepository flavorRepository;
    private final BranchRepository branchRepository;

    /** 새 재고 신청을 등록한다 (BR-001). */
    @Transactional
    public StockRequestResponse createStockRequest(Admin admin, StockRequestCreateRequest request) {
        Branch branch = requireManagedBranch(admin);

        List<Long> flavorIds = request.items().stream().map(StockRequestItemRequest::flavorId).toList();
        // 저장하기 전에 걸러내야 같은 맛이 두 줄로 들어가거나 없는 맛이 저장되는 걸 막을 수 있다.
        validateNoDuplicateFlavors(flavorIds);
        Map<Long, Flavor> flavorsById = loadFlavors(flavorIds);

        // 신청과 품목에 같은 시각을 쓰면 나중에 이력을 볼 때 한 번의 사건으로 읽힌다.
        LocalDateTime requestedAt = LocalDateTime.now();
        StockRequest stockRequest = saveNewStockRequest(admin, branch, request, requestedAt);
        List<StockRequestItem> items = createStockRequestItems(stockRequest, request.items(), flavorsById);

        return StockRequestResponse.from(stockRequest, items);
    }

    /** 내 지점의 신청 목록을 조회한다 (BR-002). status가 null이면 전체 상태를 본다. */
    public Page<StockRequestResponse> getStockRequests(Admin admin, StockRequestStatus status, Pageable pageable) {
        Long branchId = requireBranchIdOf(admin);
        Page<StockRequest> requests = status == null
                ? stockRequestRepository.findByBranch_BranchIdOrderByRequestedAtDesc(branchId, pageable)
                : stockRequestRepository.findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(
                        branchId, status, pageable);
        return mapToResponsePage(requests);
    }

    /** 본사가 아직 손대지 않은(PENDING) 신청을 지점이 스스로 취소한다. */
    @Transactional
    public void cancelStockRequest(Admin admin, Long stockRequestId) {
        Long branchId = requireBranchIdOf(admin);
        StockRequest stockRequest = findOwnedRequestForUpdate(stockRequestId, branchId);
        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 취소할 수 있습니다");
        // 조회한 엔티티는 영속 상태라 값만 바꿔 두면 트랜잭션이 끝날 때 UPDATE가 나간다.
        stockRequest.cancel();
        stockRequestRepository.update(stockRequest);
    }

    /**
     * 배송 온 물건을 실제로 받았다고 확인한다 (BR-010).
     *
     * <p>여기서는 신청 상태만 DELIVERED로 바꾼다. 재고 수량 증가는 지점 재고 화면의 입고 처리에서
     * 통 -> 그램 환산과 함께 이뤄지므로, 이 메서드가 재고까지 건드리면 같은 물량이 두 번 더해진다.
     */
    @Transactional
    public StockRequestResponse confirmStockReceipt(Admin admin, Long stockRequestId) {
        Long branchId = requireBranchIdOf(admin);
        StockRequest stockRequest = findOwnedRequestForUpdate(stockRequestId, branchId);
        requireStatus(stockRequest, StockRequestStatus.SHIPPING, "배송중인 신청만 수령 확인할 수 있습니다");

        stockRequest.confirmReceipt(admin, LocalDateTime.now());
        stockRequestRepository.update(stockRequest);

        List<StockRequestItem> items =
                stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        return StockRequestResponse.from(stockRequest, items);
    }

    // --- 아래는 위 흐름을 읽기 쉽게 나눈 보조 메서드들 ---

    /**
     * 로그인한 관리자의 소속 지점 ID를 얻는다.
     *
     * <p>인증 자체는 {@code BranchAccessService}가 이미 마쳤지만, 서비스만 따로 호출되는 경우에도
     * 안전하도록 여기서 한 번 더 확인한다.
     *
     * <p>여기서 얻는 Branch는 컨트롤러(트랜잭션 밖)에서 만들어진 지연 로딩 프록시라서,
     * 식별자인 branchId는 프록시를 열지 않고도 읽을 수 있지만 지점명 같은 다른 값은 읽을 수 없다.
     */
    private Long requireBranchIdOf(Admin admin) {
        if (admin.getBranch() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "지점 관리자만 이용할 수 있습니다");
        }
        return admin.getBranch().getBranchId();
    }

    /**
     * 트랜잭션 안에서 쓸 수 있는 Branch를 다시 읽어 온다.
     *
     * <p>신청을 저장한 뒤 응답을 만들 때 지점명을 꺼내야 하는데, 인증에서 받은 프록시는 이미 세션이
     * 닫혀 있어 그 시점에 열리지 않는다(LazyInitializationException). 그래서 저장에 쓸 Branch는
     * 반드시 이 메서드로 새로 읽은 것을 사용한다.
     */
    private Branch requireManagedBranch(Admin admin) {
        Long branchId = requireBranchIdOf(admin);
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "지점 정보를 찾을 수 없습니다"));
    }

    private void validateNoDuplicateFlavors(List<Long> flavorIds) {
        if (new HashSet<>(flavorIds).size() != flavorIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 상품을 중복해서 신청할 수 없습니다");
        }
    }

    private Map<Long, Flavor> loadFlavors(List<Long> flavorIds) {
        List<Flavor> flavors = flavorRepository.findAllById(flavorIds);
        // 요청한 개수보다 적게 조회됐다면 없는 flavorId가 섞여 있다는 뜻이다.
        if (flavors.size() != flavorIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다");
        }
        Map<Long, Flavor> flavorsById = new HashMap<>();
        for (Flavor flavor : flavors) {
            flavorsById.put(flavor.getFlavorId(), flavor);
        }
        return flavorsById;
    }

    private StockRequest saveNewStockRequest(
            Admin admin, Branch branch, StockRequestCreateRequest request, LocalDateTime requestedAt) {
        StockRequest stockRequest = StockRequest.builder()
                // request_number는 NOT NULL + UNIQUE라 INSERT 시점에 뭐라도 들어가야 한다.
                // 진짜 번호는 PK를 알아야 만들 수 있어서, 저장 후 곧바로 아래에서 덮어쓴다.
                .requestNumber(createTemporaryRequestNumber())
                .branch(branch)
                .requesterAdmin(admin)
                .requestType(RequestType.RESTOCK)
                .requestStatus(StockRequestStatus.PENDING)
                .requestReason(request.requestReason())
                .urgency(request.urgency() == null ? Urgency.NORMAL : request.urgency())
                .requestedAt(requestedAt)
                .build();
        stockRequestRepository.save(stockRequest);

        // PK가 들어가므로 같은 날 여러 건이 들어와도 번호가 겹치지 않는다.
        stockRequest.assignRequestNumber(
                "REQ-" + LocalDate.now().format(REQUEST_NUMBER_DATE_FORMAT) + "-" + stockRequest.getStockRequestId());
        return stockRequest;
    }

    private String createTemporaryRequestNumber() {
        String randomValue = UUID.randomUUID().toString().replace("-", "");
        return TEMPORARY_REQUEST_NUMBER_PREFIX
                + randomValue.substring(0, REQUEST_NUMBER_MAX_LENGTH - TEMPORARY_REQUEST_NUMBER_PREFIX.length());
    }

    private List<StockRequestItem> createStockRequestItems(
            StockRequest stockRequest, List<StockRequestItemRequest> itemRequests, Map<Long, Flavor> flavorsById) {
        List<StockRequestItem> items = new ArrayList<>();
        for (StockRequestItemRequest itemRequest : itemRequests) {
            items.add(StockRequestItem.builder()
                    .stockRequest(stockRequest)
                    .flavor(flavorsById.get(itemRequest.flavorId()))
                    .requestedQuantity(itemRequest.requestedQuantity())
                    .build());
        }
        stockRequestItemRepository.saveAll(items);
        return items;
    }

    /** 신청 행을 잠근 뒤, 정말 내 지점 것인지 확인한다. 남의 지점 건이면 403. */
    private StockRequest findOwnedRequestForUpdate(Long stockRequestId, Long branchId) {
        StockRequest stockRequest = stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
        if (!stockRequest.getBranch().getBranchId().equals(branchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 지점의 신청 건은 처리할 수 없습니다");
        }
        return stockRequest;
    }

    private void requireStatus(StockRequest stockRequest, StockRequestStatus expectedStatus, String message) {
        if (stockRequest.getRequestStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /** 신청 목록에 품목을 붙인다. 신청마다 따로 조회하면 N+1이 나서 한 번에 모아 온다. */
    private Page<StockRequestResponse> mapToResponsePage(Page<StockRequest> requests) {
        List<Long> requestIds = requests.getContent().stream().map(StockRequest::getStockRequestId).toList();
        Map<Long, List<StockRequestItem>> itemsByRequestId = requestIds.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));
        // Page.map은 전체 건수 같은 페이지 정보를 유지한 채 내용만 DTO로 바꿔 준다.
        return requests.map(stockRequest -> StockRequestResponse.from(
                stockRequest, itemsByRequestId.getOrDefault(stockRequest.getStockRequestId(), List.of())));
    }
}
