package com.kiosk.branch.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestItemRequest;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import com.kiosk.domain.inventory.InventoryTransaction;
import com.kiosk.domain.inventory.InventoryTransactionRepository;
import com.kiosk.domain.inventory.InventoryTransactionType;
import com.kiosk.domain.stockrequest.RequestType;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import com.kiosk.global.security.ActorGuard;
import com.kiosk.stockrequest.dto.StockRequestResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 지점 관점의 재고 신청 업무 흐름을 조정하는 애플리케이션 서비스다.
 *
 * <p>컨트롤러에서 받은 요청을 도메인 객체와 저장소 호출로 연결한다. 구체적으로
 * 지점 권한 확인, 신청 품목 검증, 신청 생성·취소, 배송 수령 시 재고 및 거래 이력
 * 반영을 한곳에서 순서대로 수행한다.</p>
 *
 * <p>클래스의 기본 트랜잭션은 {@code readOnly = true}다. 조회는 불필요한 변경 감지를
 * 줄이고, 데이터를 바꾸는 메서드만 별도의 {@link Transactional @Transactional}로
 * 쓰기 트랜잭션을 연다.</p>
 */
@Service
@Transactional(readOnly = true)
public class BranchStockRequestService {

    /** 사람이 읽는 신청 번호에 들어갈 날짜를 {@code yyyyMMdd} 형태로 만든다. */
    private static final DateTimeFormatter REQUEST_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** request_number 컬럼의 최대 길이와 맞춘다. */
    private static final int REQUEST_NUMBER_MAX_LENGTH = 30;
    /** 임시 번호임을 알아볼 수 있는 접두사다. */
    private static final String TEMPORARY_REQUEST_NUMBER_PREFIX = "TMP-";

    /** 재고 신청 본문(신청 지점, 상태, 처리 시각 등)을 저장하고 조회한다. */
    private final StockRequestRepository stockRequestRepository;
    /** 한 신청에 포함된 맛별 신청 수량을 저장하고 조회한다. */
    private final StockRequestItemRepository stockRequestItemRepository;
    /** 요청으로 받은 맛 기본키가 실제 상품인지 확인하는 데 사용한다. */
    private final FlavorRepository flavorRepository;
    /** 수령 확정 시 지점의 맛별 현재 재고를 잠그고 갱신한다. */
    private final BranchInventoryRepository branchInventoryRepository;
    /** 재고 수량이 변경된 이유와 결과 수량을 거래 이력으로 남긴다. */
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * 생성자 주입으로 업무에 필요한 저장소를 받는다.
     *
     * <p>필드를 {@code final}로 두면 서비스가 만들어진 뒤 의존성이 바뀌지 않아,
     * 이 클래스가 어떤 저장소를 사용하는지 생성자만 보고 파악할 수 있다.</p>
     *
     * @param stockRequestRepository 재고 신청 저장소
     * @param stockRequestItemRepository 재고 신청 품목 저장소
     * @param flavorRepository 맛 상품 저장소
     * @param branchInventoryRepository 지점 재고 저장소
     * @param inventoryTransactionRepository 재고 거래 이력 저장소
     */
    public BranchStockRequestService(
            StockRequestRepository stockRequestRepository,
            StockRequestItemRepository stockRequestItemRepository,
            FlavorRepository flavorRepository,
            BranchInventoryRepository branchInventoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.stockRequestRepository = stockRequestRepository;
        this.stockRequestItemRepository = stockRequestItemRepository;
        this.flavorRepository = flavorRepository;
        this.branchInventoryRepository = branchInventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    /**
     * 로그인한 지점 관리자 명의로 새 재고 신청과 신청 품목을 생성한다.
     *
     * <p>신청 본문과 여러 품목은 하나의 쓰기 트랜잭션에서 저장된다. 품목 저장 중 런타임
     * 예외가 발생해 트랜잭션이 롤백되면 신청 본문도 함께 되돌아가 불완전한 신청이 남지 않는다.</p>
     *
     * @param admin 신청을 등록하는 현재 관리자
     * @param request 입력 검증을 통과한 신청 내용
     * @return 저장된 신청과 품목을 조합한 응답
     */
    @Transactional
    public StockRequestResponse createStockRequest(Admin admin, StockRequestCreateRequest request) {
        // 본사 계정이 지점 기능을 호출하지 못하게 막고, 이후 조회 범위를 소속 지점으로 고정한다.
        Branch branch = ActorGuard.requireBranchOf(admin);
        List<Long> flavorIds = extractFlavorIds(request.items());

        // 저장 전에 중복 상품과 존재하지 않는 상품을 걸러 명확한 400 응답을 돌려준다.
        validateNoDuplicateFlavors(flavorIds);
        Map<Long, Flavor> flavorsById = loadFlavors(flavorIds);

        // 신청과 모든 품목에 같은 요청 시점을 사용하면 한 번의 업무 사건으로 이해하기 쉽다.
        LocalDateTime requestedAt = LocalDateTime.now();
        StockRequest stockRequest = saveNewStockRequest(admin, branch, request, requestedAt);
        List<StockRequestItem> items = createStockRequestItems(stockRequest, request.items(), flavorsById);

        // 영속성 엔티티를 API에 직접 노출하지 않고, 필요한 값만 응답 DTO로 변환한다.
        return StockRequestResponse.from(stockRequest, items);
    }

    /**
     * 현재 관리자의 소속 지점 신청만 최신순으로 조회한다.
     *
     * @param admin 조회를 요청한 현재 관리자
     * @param status 상태 필터. {@code null}이면 상태 조건을 사용하지 않는다.
     * @param pageable 요청 페이지 정보
     * @return 품목 정보를 포함한 신청 응답 페이지
     */
    public Page<StockRequestResponse> getStockRequests(Admin admin, StockRequestStatus status, Pageable pageable) {
        // 클라이언트가 branchId를 보내도록 하지 않고 인증 정보에서 지점을 얻어 조회 범위를 보호한다.
        Branch branch = ActorGuard.requireBranchOf(admin);
        Page<StockRequest> requests;

        // 선택 조건의 유무에 맞는 저장소 메서드를 골라 불필요한 동적 조건 조립을 피한다.
        if (status == null) {
            requests = stockRequestRepository.findByBranch_BranchIdOrderByRequestedAtDesc(
                    branch.getBranchId(), pageable);
        } else {
            requests = stockRequestRepository.findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(
                    branch.getBranchId(), status, pageable);
        }

        // 신청 페이지를 먼저 가져온 뒤 품목을 묶어서 조회해 신청마다 쿼리하는 N+1 문제를 피한다.
        return mapToResponsePage(requests);
    }

    /**
     * 현재 지점 소유이며 {@link StockRequestStatus#PENDING PENDING} 상태인 신청을 취소한다.
     *
     * @param admin 취소를 요청한 현재 관리자
     * @param stockRequestId 취소할 신청의 기본키
     */
    @Transactional
    public void cancelStockRequest(Admin admin, Long stockRequestId) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        // 같은 신청을 동시에 승인·취소하지 못하도록 행을 잠근 뒤 소유 지점과 상태를 확인한다.
        StockRequest stockRequest = findOwnedRequestForUpdate(stockRequestId, branch);

        requireStatus(stockRequest, StockRequestStatus.PENDING, "대기중인 신청만 취소할 수 있습니다");
        // 조회한 엔티티는 영속 상태이므로 상태 변경은 트랜잭션 종료 시 더티 체킹으로 반영된다.
        stockRequest.cancel();
    }

    /**
     * 배송 중인 신청을 수령 완료로 바꾸고, 각 품목을 지점 재고와 거래 이력에 반영한다.
     *
     * <p>신청 상태, 모든 재고 수량, 모든 거래 이력이 한 트랜잭션에 포함된다. 처리 중 런타임
     * 예외가 발생해 트랜잭션이 롤백되면 변경도 함께 되돌아가 "수령 완료인데 재고는 미반영"인
     * 상태를 방지한다.</p>
     *
     * @param admin 수령을 확정하는 현재 관리자
     * @param stockRequestId 수령할 신청의 기본키
     * @return 수령 완료 상태의 신청 응답
     */
    @Transactional
    public StockRequestResponse confirmStockReceipt(Admin admin, Long stockRequestId) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        // 신청 행을 먼저 잠가 중복 수령 처리와 본사의 동시 상태 변경을 차단한다.
        StockRequest stockRequest = findOwnedRequestForUpdate(stockRequestId, branch);

        requireStatus(stockRequest, StockRequestStatus.SHIPPING, "배송중인 신청만 수령 확인할 수 있습니다");

        // 모든 변경 기록에 동일한 수령 시각을 사용한다.
        LocalDateTime receivedAt = LocalDateTime.now();
        stockRequest.confirmReceipt(admin, receivedAt);

        // 맛 기본키 순서로 재고를 잠그면 여러 트랜잭션의 잠금 순서가 같아져 교착 위험을 줄인다.
        List<StockRequestItem> items = loadItemsInFlavorOrder(stockRequestId);
        for (StockRequestItem item : items) {
            receiveInventoryItem(branch, admin, stockRequest, item, receivedAt);
        }

        return StockRequestResponse.from(stockRequest, items);
    }

    /**
     * 신청 품목 DTO에서 맛 기본키만 순서대로 추출한다.
     *
     * @param itemRequests 클라이언트가 보낸 신청 품목
     * @return 입력 순서를 유지한 맛 기본키 목록
     */
    private List<Long> extractFlavorIds(List<StockRequestItemRequest> itemRequests) {
        List<Long> flavorIds = new ArrayList<>();
        for (StockRequestItemRequest itemRequest : itemRequests) {
            flavorIds.add(itemRequest.flavorId());
        }
        return flavorIds;
    }

    /**
     * 하나의 신청 안에 같은 맛이 두 번 들어가는 것을 거부한다.
     *
     * <p>{@link HashSet}은 중복 값을 하나로 합치므로 원본 목록과 크기가 다르면 중복이 있다.</p>
     *
     * @param flavorIds 중복 여부를 확인할 맛 기본키 목록
     */
    private void validateNoDuplicateFlavors(List<Long> flavorIds) {
        if (new HashSet<>(flavorIds).size() != flavorIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 상품을 중복해서 신청할 수 없습니다");
        }
    }

    /**
     * 요청된 맛을 한 번에 조회하고, 이후 품목 생성에 쓸 기본키-엔티티 표를 만든다.
     *
     * <p>조회 결과 수가 요청 수보다 작으면 존재하지 않는 기본키가 섞인 것이므로 저장 전에
     * 요청을 거부한다. 앞 단계에서 중복을 제거했기 때문에 단순한 크기 비교가 가능하다.</p>
     *
     * @param flavorIds 검증하고 조회할 맛 기본키 목록
     * @return 맛 기본키로 바로 찾을 수 있는 엔티티 맵
     */
    private Map<Long, Flavor> loadFlavors(List<Long> flavorIds) {
        List<Flavor> flavors = flavorRepository.findAllById(flavorIds);
        if (flavors.size() != flavorIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다");
        }

        Map<Long, Flavor> flavorsById = new HashMap<>();
        for (Flavor flavor : flavors) {
            flavorsById.put(flavor.getFlavorId(), flavor);
        }
        return flavorsById;
    }

    /**
     * 재고 신청 본문을 저장하고 외부에 보여 줄 신청 번호를 확정한다.
     *
     * <p>최종 신청 번호에는 DB가 생성하는 기본키가 필요하다. 먼저 충돌 가능성이 매우 낮은 임시 번호로
     * 저장해 기본키를 받은 뒤 {@code REQ-날짜-기본키}로 바꾼다. 두 변경은 같은 트랜잭션에
     * 있고, 영속 상태 엔티티의 번호 변경은 더티 체킹으로 UPDATE 된다.</p>
     *
     * @param admin 신청자 관리자
     * @param branch 신청자의 소속 지점
     * @param request 신청 입력값
     * @param requestedAt 신청 시각
     * @return 저장되어 기본키와 최종 신청 번호가 정해진 신청
     */
    private StockRequest saveNewStockRequest(
            Admin admin,
            Branch branch,
            StockRequestCreateRequest request,
            LocalDateTime requestedAt) {
        StockRequest stockRequest = StockRequest.builder()
                .requestNumber(createTemporaryRequestNumber())
                .branch(branch)
                .requesterAdmin(admin)
                .requestType(RequestType.RESTOCK)
                .requestStatus(StockRequestStatus.PENDING)
                .requestReason(request.requestReason())
                .urgency(request.urgency() == null ? Urgency.NORMAL : request.urgency())
                .requestedAt(requestedAt)
                .build();

        // INSERT 후 자동 생성된 stockRequestId를 엔티티에서 사용할 수 있다.
        stockRequestRepository.save(stockRequest);

        // 기본키를 포함하므로 날짜가 같은 여러 신청도 서로 다른 업무용 번호를 갖는다.
        String requestNumber = "REQ-"
                + LocalDate.now().format(REQUEST_NUMBER_DATE_FORMAT)
                + "-"
                + stockRequest.getStockRequestId();
        stockRequest.assignRequestNumber(requestNumber);
        return stockRequest;
    }

    /**
     * 최초 INSERT의 유일성 제약을 만족시키기 위한 충돌 가능성이 매우 낮은 임시 번호를 만든다.
     *
     * <p>전체 UUID를 붙이면 {@code request_number VARCHAR(30)}보다 길어진다. 따라서 하이픈을
     * 제거한 UUID에서 컬럼에 들어갈 수 있는 길이만 사용한다. 남는 난수는 104비트이므로 임시
     * INSERT 사이에서 충돌할 가능성은 매우 낮고, 저장 직후에는 PK 기반 최종 번호로 교체된다.</p>
     *
     * @return 최대 30자인 {@code TMP-난수} 형식의 임시 번호
     */
    private String createTemporaryRequestNumber() {
        String randomValue = UUID.randomUUID().toString().replace("-", "");
        int randomValueLength = REQUEST_NUMBER_MAX_LENGTH - TEMPORARY_REQUEST_NUMBER_PREFIX.length();
        return TEMPORARY_REQUEST_NUMBER_PREFIX + randomValue.substring(0, randomValueLength);
    }

    /**
     * 검증된 입력 DTO를 신청 품목 엔티티로 바꾸어 한 번에 저장한다.
     *
     * @param stockRequest 각 품목이 속할 재고 신청
     * @param itemRequests 품목별 맛 기본키와 신청 수량
     * @param flavorsById 앞에서 존재 여부를 검증한 맛 엔티티 맵
     * @return 저장된 신청 품목 목록
     */
    private List<StockRequestItem> createStockRequestItems(
            StockRequest stockRequest,
            List<StockRequestItemRequest> itemRequests,
            Map<Long, Flavor> flavorsById) {
        List<StockRequestItem> items = new ArrayList<>();

        // DTO의 flavorId를 검증된 Flavor 엔티티로 치환해 연관관계를 완성한다.
        for (StockRequestItemRequest itemRequest : itemRequests) {
            StockRequestItem item = StockRequestItem.builder()
                    .stockRequest(stockRequest)
                    .flavor(flavorsById.get(itemRequest.flavorId()))
                    .requestedQuantity(itemRequest.requestedQuantity())
                    .build();
            items.add(item);
        }

        stockRequestItemRepository.saveAll(items);
        return items;
    }

    /**
     * 신청 행을 쓰기 잠금으로 읽은 뒤 현재 지점의 신청인지 확인한다.
     *
     * <p>존재하지 않으면 404, 존재하지만 다른 지점 소유이면 403으로 원인을 구분한다.</p>
     *
     * @param stockRequestId 조회할 신청 기본키
     * @param branch 현재 관리자의 소속 지점
     * @return 잠금이 획득되고 소유권이 확인된 신청
     */
    private StockRequest findOwnedRequestForUpdate(Long stockRequestId, Branch branch) {
        StockRequest stockRequest = stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));

        if (!stockRequest.getBranch().getBranchId().equals(branch.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 지점의 신청 건은 처리할 수 없습니다");
        }
        return stockRequest;
    }

    /**
     * 상태 전환을 실행하기 전에 현재 상태가 기대 상태인지 확인한다.
     *
     * @param stockRequest 검사할 신청
     * @param expectedStatus 해당 동작이 허용되는 상태
     * @param message 상태가 맞지 않을 때 사용자에게 전달할 설명
     */
    private void requireStatus(StockRequest stockRequest, StockRequestStatus expectedStatus, String message) {
        if (stockRequest.getRequestStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /**
     * 신청 품목을 맛 기본키 오름차순으로 정렬한다.
     *
     * <p>수령 처리에서 이 순서대로 각 재고 행을 잠그므로, 동시 트랜잭션도 같은 순서로
     * 잠금에 접근하게 되어 교착 상태 가능성을 낮춘다.</p>
     *
     * @param stockRequestId 품목을 조회할 신청 기본키
     * @return 맛 기본키 순으로 정렬된 품목 목록
     */
    private List<StockRequestItem> loadItemsInFlavorOrder(Long stockRequestId) {
        List<StockRequestItem> items = new ArrayList<>(
                stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId));
        items.sort(Comparator.comparing(item -> item.getFlavor().getFlavorId()));
        return items;
    }

    /**
     * 신청 품목 하나의 승인 수량을 실제 지점 재고에 더하고 거래 이력을 남긴다.
     *
     * <p>현재 수량 변경과 이력 INSERT가 같은 트랜잭션에서 실행되어, 재고의 현재값과
     * 증감 내역이 서로 어긋나지 않는다.</p>
     *
     * @param branch 재고를 받을 지점
     * @param admin 수령을 처리한 관리자
     * @param stockRequest 원인이 된 재고 신청
     * @param item 반영할 신청 품목
     * @param receivedAt 모든 수령 변경에 공통으로 기록할 시각
     */
    private void receiveInventoryItem(
            Branch branch,
            Admin admin,
            StockRequest stockRequest,
            StockRequestItem item,
            LocalDateTime receivedAt) {
        // 현재 트랜잭션이 해당 맛의 재고 행에 쓰기 락을 건다.
        // 모든 재고 변경 흐름이 같은 잠금 규칙을 사용할 때 동시 갱신 유실을 막을 수 있다.
        BranchInventory inventory = findInventoryForUpdate(branch, item.getFlavor());
        int receivedQuantity = item.getQuantityToReceive();

        inventory.receive(receivedQuantity);

        // 변경 후 수량까지 기록하면 나중에 재고 변동의 원인과 결과를 추적할 수 있다.
        InventoryTransaction transaction = InventoryTransaction.builder()
                .branch(branch)
                .branchInventory(inventory)
                .flavor(item.getFlavor())
                .transactionType(InventoryTransactionType.REQUEST_RECEIVED)
                .changeQuantity(receivedQuantity)
                .quantityAfter(inventory.getCurrentQuantity())
                .reason("재고 신청 수령확인 (" + stockRequest.getRequestNumber() + ")")
                .stockRequest(stockRequest)
                .processedAdmin(admin)
                .transactionAt(receivedAt)
                .build();
        inventoryTransactionRepository.save(transaction);
    }

    /**
     * 지점과 맛의 조합에 해당하는 재고 행을 쓰기 잠금으로 조회한다.
     *
     * @param branch 재고를 소유한 지점
     * @param flavor 입고할 맛
     * @return 현재 트랜잭션이 쓰기 락을 획득한 지점 재고
     */
    private BranchInventory findInventoryForUpdate(Branch branch, Flavor flavor) {
        return branchInventoryRepository
                .findByBranchAndFlavorForUpdate(branch.getBranchId(), flavor.getFlavorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "지점 재고 항목을 찾을 수 없습니다: " + flavor.getFlavorName()));
    }

    /**
     * 신청 엔티티 페이지와 품목들을 합쳐 API 응답 페이지로 변환한다.
     *
     * <p>페이지 안의 신청 기본키를 먼저 모아 품목을 한 번에 조회한 뒤 신청별로 그룹화한다.
     * 신청별 품목 조회 N+1을 피하므로 품목 조회 쿼리 수는 페이지 크기와 무관하게 유지된다.
     * 다만 응답 변환에서 접근하는 다른 LAZY 관계는 별도의 fetch 전략이 필요할 수 있다.</p>
     *
     * @param requests DB에서 조회한 신청 페이지
     * @return 각 신청에 품목 목록이 결합된 응답 페이지
     */
    private Page<StockRequestResponse> mapToResponsePage(Page<StockRequest> requests) {
        // 현재 페이지에 실제로 포함된 신청만 품목 조회 대상으로 삼는다.
        List<Long> requestIds = requests.getContent().stream()
                .map(StockRequest::getStockRequestId)
                .toList();

        // 빈 페이지라면 IN 쿼리를 생략하고 빈 맵을 사용한다.
        Map<Long, List<StockRequestItem>> itemsByRequestId = requestIds.isEmpty()
                ? Map.of()
                : stockRequestItemRepository.findByStockRequestIdIn(requestIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getStockRequest().getStockRequestId()));

        // Page.map은 전체 건수와 페이지 메타데이터를 보존하면서 내용만 DTO로 바꾼다.
        return requests.map(stockRequest -> StockRequestResponse.from(
                stockRequest,
                itemsByRequestId.getOrDefault(stockRequest.getStockRequestId(), List.of())));
    }
}
