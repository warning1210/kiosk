package com.kiosk.branch.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestItemRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import com.kiosk.domain.inventory.InventoryStatus;
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
import com.kiosk.global.exception.BusinessException;
import com.kiosk.global.exception.ErrorCode;
import com.kiosk.global.security.ActorGuard;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BranchStockRequestService {

    private static final DateTimeFormatter REQUEST_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;
    private final FlavorRepository flavorRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public BranchStockRequestService(StockRequestRepository stockRequestRepository,
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

    @Transactional
    public StockRequestResponse create(Admin admin, StockRequestCreateRequest request) {
        Branch branch = ActorGuard.requireBranchOf(admin);

        List<Long> flavorIds = request.items().stream().map(StockRequestItemRequest::flavorId).distinct().toList();
        Map<Long, Flavor> flavorsById = flavorRepository.findAllById(flavorIds).stream()
                .collect(Collectors.toMap(Flavor::getFlavorId, f -> f));
        if (flavorsById.size() != flavorIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 상품이 포함되어 있습니다");
        }

        LocalDateTime now = LocalDateTime.now();
        StockRequest stockRequest = StockRequest.builder()
                .requestNumber("TMP-" + System.nanoTime())
                .branch(branch)
                .requesterAdmin(admin)
                .requestType(RequestType.RESTOCK)
                .requestStatus(StockRequestStatus.PENDING)
                .requestReason(request.requestReason())
                .urgency(request.urgency() != null ? request.urgency() : Urgency.NORMAL)
                .requestedAt(now)
                .build();
        stockRequestRepository.save(stockRequest);
        stockRequest.setRequestNumber("REQ-" + LocalDate.now().format(REQUEST_NUMBER_DATE_FORMAT) + "-"
                + stockRequest.getStockRequestId());

        List<StockRequestItem> items = request.items().stream()
                .map(itemRequest -> StockRequestItem.builder()
                        .stockRequest(stockRequest)
                        .flavor(flavorsById.get(itemRequest.flavorId()))
                        .requestedQuantity(itemRequest.requestedQuantity())
                        .build())
                .toList();
        stockRequestItemRepository.saveAll(items);

        return StockRequestResponse.from(stockRequest, items);
    }

    public Page<StockRequestResponse> list(Admin admin, StockRequestStatus status, Pageable pageable) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        Page<StockRequest> page = status != null
                ? stockRequestRepository.findByBranch_BranchIdAndRequestStatus(branch.getBranchId(), status, pageable)
                : stockRequestRepository.findByBranch_BranchId(branch.getBranchId(), pageable);
        return toResponsePage(page);
    }

    @Transactional
    public void cancel(Admin admin, Long stockRequestId) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        StockRequest existing = loadOwned(stockRequestId, branch);
        if (existing.getRequestStatus() != StockRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "대기중인 신청만 취소할 수 있습니다");
        }
        existing.setRequestStatus(StockRequestStatus.CLOSED);
    }

    @Transactional
    public StockRequestResponse confirmReceipt(Admin admin, Long stockRequestId) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        StockRequest existing = loadOwned(stockRequestId, branch);
        if (existing.getRequestStatus() != StockRequestStatus.SHIPPING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "배송중인 신청만 수령 확인할 수 있습니다");
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setRequestStatus(StockRequestStatus.DELIVERED);
        existing.setReceiptConfirmedAdmin(admin);
        existing.setDeliveredAt(now);

        List<StockRequestItem> items = stockRequestItemRepository.findByStockRequest_StockRequestId(stockRequestId);
        for (StockRequestItem item : items) {
            int receivedQuantity = item.getApprovedQuantity() != null ? item.getApprovedQuantity() : item.getRequestedQuantity();
            BranchInventory inventory = branchInventoryRepository
                    .findByBranch_BranchIdAndFlavor_FlavorId(branch.getBranchId(), item.getFlavor().getFlavorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "지점 재고 항목을 찾을 수 없습니다: " + item.getFlavor().getFlavorName()));

            int newQuantity = inventory.getCurrentQuantity() + receivedQuantity;
            inventory.setCurrentQuantity(newQuantity);
            inventory.setInventoryStatus(resolveInventoryStatus(newQuantity, inventory.getSafetyQuantity()));

            inventoryTransactionRepository.save(InventoryTransaction.builder()
                    .branch(branch)
                    .branchInventory(inventory)
                    .flavor(item.getFlavor())
                    .transactionType(InventoryTransactionType.REQUEST_RECEIVED)
                    .changeQuantity(receivedQuantity)
                    .quantityAfter(newQuantity)
                    .reason("재고 신청 수령확인 (" + existing.getRequestNumber() + ")")
                    .stockRequest(existing)
                    .processedAdmin(admin)
                    .transactionAt(now)
                    .build());
        }

        return StockRequestResponse.from(existing, items);
    }

    private StockRequest loadOwned(Long stockRequestId, Branch branch) {
        StockRequest existing = stockRequestRepository.findByIdForUpdate(stockRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신청 내역을 찾을 수 없습니다"));
        if (!existing.getBranch().getBranchId().equals(branch.getBranchId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "다른 지점의 신청 건은 처리할 수 없습니다");
        }
        return existing;
    }

    private InventoryStatus resolveInventoryStatus(int currentQuantity, int safetyQuantity) {
        if (currentQuantity <= 0) {
            return InventoryStatus.SOLD_OUT;
        }
        if (currentQuantity < safetyQuantity) {
            return InventoryStatus.LOW;
        }
        return InventoryStatus.NORMAL;
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
