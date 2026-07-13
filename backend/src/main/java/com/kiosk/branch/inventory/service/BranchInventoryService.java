package com.kiosk.branch.inventory.service;

import com.kiosk.branch.inventory.dto.InventoryAdjustRequest;
import com.kiosk.branch.inventory.dto.InventoryResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import com.kiosk.domain.inventory.InventoryStatus;
import com.kiosk.domain.inventory.InventoryTransaction;
import com.kiosk.domain.inventory.InventoryTransactionRepository;
import com.kiosk.domain.inventory.InventoryTransactionType;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.stockrequest.RequestType;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 아이스크림은 무게(g) 단위로 퍼서 파는 상품이라 재고를 그램 단위로 관리한다.
// 통 하나 = TUB_GRAMS(안전재고 기준), 새 통 개봉 시 TARGET_GRAMS로 채워진다고 가정.
// 실제 스쿱 오차는 여기서 보정하지 않고(이론재고), 지점이 실사로 SET 조정하는 걸로 흡수한다.
@Service
@RequiredArgsConstructor
@Transactional
public class BranchInventoryService {

    private static final int TUB_GRAMS = 3000;
    private static final int TARGET_GRAMS = 6000;
    private static final List<StockRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
            StockRequestStatus.PENDING, StockRequestStatus.APPROVED,
            StockRequestStatus.PREPARING, StockRequestStatus.SHIPPING, StockRequestStatus.DELIVERED);

    private final BranchRepository branchRepository;
    private final AdminRepository adminRepository;
    private final FlavorRepository flavorRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StockRequestRepository stockRequestRepository;
    private final StockRequestItemRepository stockRequestItemRepository;
    private final OrderItemFlavorRepository orderItemFlavorRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventory(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("지점을 찾을 수 없습니다."));
        return inventoryRepository.findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(branch.getBranchId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public void adjustInventory(Long inventoryId, InventoryAdjustRequest request, Long branchId) {
        BranchInventory inventory = requireOwnedInventory(inventoryId, branchId);
        int grams = request.grams() == null ? 0 : request.grams();
        int before = inventory.getCurrentQuantity();
        int after = switch (request.type() == null ? "" : request.type()) {
            case "IN" -> before + grams;
            case "SET" -> Math.max(0, grams);
            default -> Math.max(0, before - grams);
        };
        applyQuantity(inventory, after);
        transactionRepository.save(InventoryTransaction.builder()
                .branch(inventory.getBranch()).branchInventory(inventory).flavor(inventory.getFlavor())
                .transactionType("IN".equals(request.type()) ? InventoryTransactionType.IN : InventoryTransactionType.ADJUST)
                .changeQuantity(after - before).quantityAfter(after).reason("지점 재고 수동 조정")
                .transactionAt(LocalDateTime.now()).build());
        if (after <= TUB_GRAMS) {
            createAutomaticRequestIfNeeded(inventory.getBranch(), inventory.getFlavor());
        }
    }

    public void receiveInventory(Long inventoryId, Long branchId) {
        BranchInventory inventory = requireOwnedInventory(inventoryId, branchId);
        List<StockRequestItem> active = stockRequestItemRepository.findActive(
                inventory.getBranch().getBranchId(), inventory.getFlavor().getFlavorId(), List.of(StockRequestStatus.DELIVERED));
        if (active.isEmpty()) {
            throw new IllegalStateException("본점에서 배송 완료 처리한 입고 건이 없습니다.");
        }
        StockRequestItem requestItem = active.get(0);
        int added = requestItem.getRequestedQuantity() * TUB_GRAMS;
        int after = inventory.getCurrentQuantity() + added;
        applyQuantity(inventory, after);

        StockRequest request = requestItem.getStockRequest();
        request.setRequestStatus(StockRequestStatus.CLOSED);
        stockRequestRepository.save(request);

        transactionRepository.save(InventoryTransaction.builder()
                .branch(inventory.getBranch()).branchInventory(inventory).flavor(inventory.getFlavor())
                .transactionType(InventoryTransactionType.REQUEST_RECEIVED)
                .changeQuantity(added).quantityAfter(after).reason("자동 발주 입고 완료")
                .stockRequest(request).transactionAt(LocalDateTime.now()).build());
    }

    // 결제가 확정된 주문의 각 맛(flavor)별로 재고를 차감한다. PaymentService.confirm()에서 호출된다.
    public void deductForOrder(Order order) {
        Branch branch = order.getBranch();
        for (OrderItem item : order.getOrderItems()) {
            List<OrderItemFlavor> flavors = orderItemFlavorRepository
                    .findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(item.getOrderItemId());
            if (flavors.isEmpty()) continue;
            int totalGrams = gramsFor(item.getProductNameSnapshot()) * item.getQuantity();
            int perFlavor = totalGrams / flavors.size();
            int remainder = totalGrams % flavors.size();
            for (int index = 0; index < flavors.size(); index++) {
                Flavor flavor = flavors.get(index).getFlavor();
                int grams = perFlavor + (index < remainder ? 1 : 0);
                deductOne(branch, flavor, grams, order);
            }
        }
    }

    private void deductOne(Branch branch, Flavor flavor, int grams, Order order) {
        BranchInventory inventory = inventoryRepository.findByBranch_BranchIdAndFlavor_FlavorId(branch.getBranchId(), flavor.getFlavorId())
                .orElseGet(() -> inventoryRepository.save(BranchInventory.builder()
                        .branch(branch).flavor(flavor)
                        .currentQuantity(TARGET_GRAMS).safetyQuantity(TUB_GRAMS)
                        .inventoryStatus(InventoryStatus.NORMAL).build()));
        int after = Math.max(0, inventory.getCurrentQuantity() - grams);
        applyQuantity(inventory, after);
        transactionRepository.save(InventoryTransaction.builder()
                .branch(branch).branchInventory(inventory).flavor(flavor)
                .transactionType(InventoryTransactionType.ORDER)
                .changeQuantity(-grams).quantityAfter(after).reason("키오스크 주문 자동 차감")
                .order(order).transactionAt(LocalDateTime.now()).build());
        if (after <= TUB_GRAMS) {
            createAutomaticRequestIfNeeded(branch, flavor);
        }
    }

    private void applyQuantity(BranchInventory inventory, int after) {
        inventory.setCurrentQuantity(after);
        inventory.setInventoryStatus(after == 0 ? InventoryStatus.SOLD_OUT
                : after <= inventory.getSafetyQuantity() ? InventoryStatus.LOW : InventoryStatus.NORMAL);
        inventory.setIsKioskVisible(after > 0);
        inventoryRepository.save(inventory);
    }

    // 안전재고 이하로 떨어지면, 이미 대기 중인 신청이 없을 때만 자동으로 재고 신청을 만든다 (BR-024).
    private void createAutomaticRequestIfNeeded(Branch branch, Flavor flavor) {
        if (!stockRequestItemRepository.findActive(branch.getBranchId(), flavor.getFlavorId(), ACTIVE_REQUEST_STATUSES).isEmpty()) {
            return;
        }
        Admin requester = adminRepository.findFirstByBranch_BranchIdOrderByAdminIdAsc(branch.getBranchId())
                .or(adminRepository::findFirstByOrderByAdminIdAsc)
                .orElse(null);
        if (requester == null) return;

        LocalDateTime now = LocalDateTime.now();
        StockRequest stockRequest = stockRequestRepository.save(StockRequest.builder()
                .requestNumber("AUTO-" + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")))
                .branch(branch).requesterAdmin(requester).requestType(RequestType.RESTOCK)
                .requestStatus(StockRequestStatus.PENDING).requestReason("재고 " + (TUB_GRAMS / 1000) + "kg 이하 자동 신청")
                .urgency(Urgency.HIGH).requestedAt(now).estimatedArrivalAt(now.plusDays(3)).build());
        stockRequestItemRepository.save(StockRequestItem.builder()
                .stockRequest(stockRequest).flavor(flavor).requestedQuantity(1).build());
    }

    private BranchInventory requireOwnedInventory(Long inventoryId, Long branchId) {
        BranchInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));
        if (!inventory.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("다른 지점의 재고는 변경할 수 없습니다.");
        }
        return inventory;
    }

    private InventoryResponse toResponse(BranchInventory inventory) {
        List<StockRequestItem> active = stockRequestItemRepository.findActive(
                inventory.getBranch().getBranchId(), inventory.getFlavor().getFlavorId(), ACTIVE_REQUEST_STATUSES);
        StockRequest request = active.isEmpty() ? null : active.get(0).getStockRequest();
        return new InventoryResponse(
                inventory.getBranchInventoryId(), inventory.getFlavor().getFlavorId(), inventory.getFlavor().getFlavorName(),
                inventory.getFlavor().getImageUrl(), inventory.getCurrentQuantity(), inventory.getInventoryStatus().name(),
                request == null ? null : request.getRequestStatus().name(),
                request == null ? null : request.getEstimatedArrivalAt(), inventory.getUpdatedAt());
    }

    // 상품별 1개당 대략적인 무게(g). 실제 계량값으로 나중에 보정 가능하도록 상수로만 분리.
    private int gramsFor(String productName) {
        String name = productName.replace(" ", "");
        if (name.contains("하프갤런") || name.contains("하프갤론")) return 1237;
        if (name.contains("패밀리")) return 989;
        if (name.contains("쿼터")) return 643;
        if (name.contains("파인트")) return 336;
        if (name.contains("더블레귤러")) return 230;
        if (name.contains("더블주니어")) return 150;
        if (name.contains("싱글킹")) return 145;
        return 115;
    }
}
