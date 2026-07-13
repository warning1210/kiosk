package com.kiosk.application;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.common.Language;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.inventory.*;
import com.kiosk.domain.order.*;
import com.kiosk.domain.payment.*;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.domain.stockrequest.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowService {
    private static final int TUB_GRAMS = 3000;
    private static final int TARGET_GRAMS = 6000;
    private static final List<StockRequestStatus> ACTIVE_REQUESTS = List.of(
            StockRequestStatus.PENDING, StockRequestStatus.APPROVED,
            StockRequestStatus.PREPARING, StockRequestStatus.SHIPPING, StockRequestStatus.DELIVERED);

    private final BranchRepository branches;
    private final AdminRepository admins;
    private final ProductRepository products;
    private final FlavorRepository flavors;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderItemFlavorRepository orderItemFlavors;
    private final PaymentRepository payments;
    private final BranchInventoryRepository inventories;
    private final InventoryTransactionRepository transactions;
    private final StockRequestRepository stockRequests;
    private final StockRequestItemRepository stockRequestItems;

    public OrderWorkflowService(BranchRepository branches, AdminRepository admins, ProductRepository products,
            FlavorRepository flavors, OrderRepository orders, OrderItemRepository orderItems,
            OrderItemFlavorRepository orderItemFlavors, PaymentRepository payments,
            BranchInventoryRepository inventories, InventoryTransactionRepository transactions,
            StockRequestRepository stockRequests, StockRequestItemRepository stockRequestItems) {
        this.branches = branches; this.admins = admins; this.products = products; this.flavors = flavors;
        this.orders = orders; this.orderItems = orderItems; this.orderItemFlavors = orderItemFlavors;
        this.payments = payments; this.inventories = inventories; this.transactions = transactions;
        this.stockRequests = stockRequests; this.stockRequestItems = stockRequestItems;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Branch branch = resolveBranch(request.branchId());
        if (request.items() == null || request.items().isEmpty()) throw new IllegalArgumentException("주문 상품이 없습니다.");
        int before = request.items().stream().mapToInt(i -> {
            Product p = products.findById(i.productId()).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            return p.getBasePrice() * Math.max(1, Optional.ofNullable(i.quantity()).orElse(1));
        }).sum();
        int discount = Math.max(0, Optional.ofNullable(request.discountAmount()).orElse(0));
        LocalDateTime now = LocalDateTime.now();
        Order order = orders.save(Order.builder()
                .branch(branch).kioskCode(Optional.ofNullable(request.kioskCode()).orElse("KIOSK-01"))
                .orderNumber("K" + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")))
                .orderType(OrderType.valueOf(Optional.ofNullable(request.orderType()).orElse("DINE_IN")))
                .orderStatus(OrderStatus.PAID).language(Language.ko)
                .amountBeforeDiscount(before).discountAmount(discount).finalAmount(Math.max(0, before - discount)).build());
        order.setWaitingNumber((int) ((order.getOrderId() - 1) % 900) + 1);

        for (CreateOrderItemRequest requestedItem : request.items()) {
            Product product = products.findById(requestedItem.productId()).orElseThrow();
            int quantity = Math.max(1, Optional.ofNullable(requestedItem.quantity()).orElse(1));
            OrderItem item = orderItems.save(OrderItem.builder().order(order).product(product)
                    .productNameSnapshot(product.getProductName()).unitPriceSnapshot(product.getBasePrice())
                    .quantity(quantity).itemTotal(product.getBasePrice() * quantity).containerType(ContainerType.CUP).build());
            List<Long> flavorIds = Optional.ofNullable(requestedItem.flavorIds()).orElse(List.of());
            int totalGrams = gramsFor(product.getProductName()) * quantity;
            int perFlavor = flavorIds.isEmpty() ? 0 : totalGrams / flavorIds.size();
            int remainder = flavorIds.isEmpty() ? 0 : totalGrams % flavorIds.size();
            for (int index = 0; index < flavorIds.size(); index++) {
                Flavor flavor = flavors.findById(flavorIds.get(index)).orElseThrow(() -> new IllegalArgumentException("맛을 찾을 수 없습니다."));
                orderItemFlavors.save(OrderItemFlavor.builder().orderItem(item).flavor(flavor)
                        .flavorNameSnapshot(flavor.getFlavorName()).selectionOrder(index + 1).quantity(1).build());
                deductInventory(branch, flavor, perFlavor + (index < remainder ? 1 : 0), order);
            }
        }
        PaymentMethod method = "TOSS".equals(request.paymentMethod()) ? PaymentMethod.QR
                : PaymentMethod.valueOf(Optional.ofNullable(request.paymentMethod()).orElse("CARD"));
        payments.save(Payment.builder().order(order).paymentMethod(method).paymentStatus(PaymentStatus.PAID)
                .requestedAmount(order.getFinalAmount()).paidAmount(order.getFinalAmount())
                .approvalNumber("DEMO-" + order.getOrderId()).paidAt(now).build());
        return new CreateOrderResponse(order.getOrderId(), order.getOrderNumber(), order.getWaitingNumber(), order.getFinalAmount());
    }

    private void deductInventory(Branch branch, Flavor flavor, int grams, Order order) {
        BranchInventory inventory = inventories.findByBranch_BranchIdAndFlavor_FlavorId(branch.getBranchId(), flavor.getFlavorId())
                .orElseGet(() -> inventories.save(BranchInventory.builder().branch(branch).flavor(flavor)
                        .currentQuantity(TARGET_GRAMS).safetyQuantity(TUB_GRAMS).inventoryStatus(InventoryStatus.NORMAL).build()));
        int after = Math.max(0, inventory.getCurrentQuantity() - grams);
        inventory.setCurrentQuantity(after);
        inventory.setInventoryStatus(after == 0 ? InventoryStatus.SOLD_OUT : after <= TUB_GRAMS ? InventoryStatus.LOW : InventoryStatus.NORMAL);
        inventory.setIsKioskVisible(after > 0);
        inventories.save(inventory);
        transactions.save(InventoryTransaction.builder().branch(branch).branchInventory(inventory).flavor(flavor)
                .transactionType(InventoryTransactionType.ORDER).changeQuantity(-grams).quantityAfter(after)
                .reason("키오스크 주문 자동 차감").order(order).transactionAt(LocalDateTime.now()).build());
        if (after <= TUB_GRAMS) createAutomaticRequest(branch, flavor);
    }

    private void createAutomaticRequest(Branch branch, Flavor flavor) {
        if (!stockRequestItems.findActive(branch.getBranchId(), flavor.getFlavorId(), ACTIVE_REQUESTS).isEmpty()) return;
        Admin requester = admins.findFirstByBranch_BranchIdOrderByAdminIdAsc(branch.getBranchId())
                .or(() -> admins.findFirstByOrderByAdminIdAsc()).orElse(null);
        if (requester == null) return;
        LocalDateTime now = LocalDateTime.now();
        StockRequest stockRequest = stockRequests.save(StockRequest.builder()
                .requestNumber("AUTO-" + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")))
                .branch(branch).requesterAdmin(requester).requestType(RequestType.RESTOCK)
                .requestStatus(StockRequestStatus.PENDING).requestReason("재고 3kg 이하 자동 신청")
                .urgency(Urgency.HIGH).requestedAt(now).estimatedArrivalAt(now.plusDays(3)).build());
        stockRequestItems.save(StockRequestItem.builder().stockRequest(stockRequest).flavor(flavor).requestedQuantity(1).build());
    }

    @Transactional
    public List<InventoryResponse> getInventory(Long branchId) {
        Branch branch = resolveBranch(branchId);
        List<Flavor> visible = flavors.findBySourceUrlIsNotNullAndIsVisibleTrueOrderByFlavorIdAsc();
        for (Flavor flavor : visible) inventories.findByBranch_BranchIdAndFlavor_FlavorId(branch.getBranchId(), flavor.getFlavorId())
                .orElseGet(() -> inventories.save(BranchInventory.builder().branch(branch).flavor(flavor)
                        .currentQuantity(TARGET_GRAMS).safetyQuantity(TUB_GRAMS).inventoryStatus(InventoryStatus.NORMAL).build()));
        return inventories.findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(branch.getBranchId()).stream().map(i -> {
            List<StockRequestItem> active = stockRequestItems.findActive(branch.getBranchId(), i.getFlavor().getFlavorId(), ACTIVE_REQUESTS);
            StockRequest request = active.isEmpty() ? null : active.get(0).getStockRequest();
            return new InventoryResponse(i.getBranchInventoryId(), i.getFlavor().getFlavorId(), i.getFlavor().getFlavorName(),
                    i.getFlavor().getImageUrl(), i.getCurrentQuantity(), i.getInventoryStatus().name(),
                    request == null ? null : request.getRequestStatus().name(),
                    request == null ? null : request.getEstimatedArrivalAt(), i.getUpdatedAt());
        }).toList();
    }

    @Transactional
    public void adjustInventory(Long inventoryId, String type, int grams, Long branchId) {
        BranchInventory inventory = inventories.findById(inventoryId).orElseThrow();
        if(!inventory.getBranch().getBranchId().equals(branchId))throw new SecurityException("다른 지점의 재고는 변경할 수 없습니다.");
        int before = inventory.getCurrentQuantity();
        int after = switch (type) {
            case "IN" -> before + grams;
            case "SET" -> Math.max(0, grams);
            default -> Math.max(0, before - grams);
        };
        inventory.setCurrentQuantity(after);
        inventory.setInventoryStatus(after == 0 ? InventoryStatus.SOLD_OUT : after <= TUB_GRAMS ? InventoryStatus.LOW : InventoryStatus.NORMAL);
        inventory.setIsKioskVisible(after > 0);
        inventories.save(inventory);
        transactions.save(InventoryTransaction.builder().branch(inventory.getBranch()).branchInventory(inventory)
                .flavor(inventory.getFlavor()).transactionType("IN".equals(type) ? InventoryTransactionType.IN : InventoryTransactionType.ADJUST)
                .changeQuantity(after - before).quantityAfter(after).reason("지점 재고 수동 조정")
                .transactionAt(LocalDateTime.now()).build());
        if (after <= TUB_GRAMS) createAutomaticRequest(inventory.getBranch(), inventory.getFlavor());
    }

    @Transactional
    public void receiveInventory(Long inventoryId, Long branchId) {
        BranchInventory inventory = inventories.findById(inventoryId).orElseThrow();
        if(!inventory.getBranch().getBranchId().equals(branchId))throw new SecurityException("다른 지점의 입고는 처리할 수 없습니다.");
        List<StockRequestItem> active = stockRequestItems.findActive(inventory.getBranch().getBranchId(),
                inventory.getFlavor().getFlavorId(), List.of(StockRequestStatus.DELIVERED));
        if (active.isEmpty()) throw new IllegalStateException("본점에서 배송 완료 처리한 입고 건이 없습니다.");
        StockRequestItem requestItem = active.get(0);
        int added = requestItem.getRequestedQuantity() * TUB_GRAMS;
        int after = inventory.getCurrentQuantity() + added;
        inventory.setCurrentQuantity(after);
        inventory.setInventoryStatus(after <= TUB_GRAMS ? InventoryStatus.LOW : InventoryStatus.NORMAL);
        inventories.save(inventory);
        StockRequest request = requestItem.getStockRequest();
        request.setRequestStatus(StockRequestStatus.CLOSED);
        stockRequests.save(request);
        transactions.save(InventoryTransaction.builder().branch(inventory.getBranch()).branchInventory(inventory)
                .flavor(inventory.getFlavor()).transactionType(InventoryTransactionType.REQUEST_RECEIVED)
                .changeQuantity(added).quantityAfter(after).reason("자동 발주 입고 완료").stockRequest(request)
                .transactionAt(LocalDateTime.now()).build());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long branchId) {
        Branch branch = resolveBranch(branchId);
        return orders.findTop100ByBranch_BranchIdOrderByCreatedAtDesc(branch.getBranchId()).stream().map(order -> {
            List<String> menus = new ArrayList<>();
            for (OrderItem item : orderItems.findByOrder_OrderIdOrderByOrderItemIdAsc(order.getOrderId())) {
                String names = orderItemFlavors.findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(item.getOrderItemId()).stream()
                        .map(OrderItemFlavor::getFlavorNameSnapshot).reduce((a,b) -> a + ", " + b).orElse("");
                menus.add(item.getProductNameSnapshot() + (names.isBlank() ? "" : " · " + names));
            }
            return new OrderResponse(order.getOrderId(), order.getWaitingNumber(), order.getOrderNumber(), order.getOrderType().name(),
                    order.getOrderStatus().name(), String.join(" / ", menus), order.getFinalAmount(), order.getCreatedAt(),
                    Math.max(0, Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes()));
        }).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status, Long branchId) {
        Order order = orders.findById(orderId).orElseThrow();
        if(!order.getBranch().getBranchId().equals(branchId))throw new SecurityException("다른 지점의 주문은 변경할 수 없습니다.");
        order.setOrderStatus(OrderStatus.valueOf(status));
        if (order.getOrderStatus() == OrderStatus.COMPLETED) order.setOrderCompletedAt(LocalDateTime.now());
        orders.save(order);
        return getOrders(order.getBranch().getBranchId()).stream().filter(o -> o.orderId().equals(orderId)).findFirst().orElseThrow();
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId != null) return branches.findById(branchId).orElseThrow(() -> new IllegalArgumentException("지점을 찾을 수 없습니다."));
        return branches.findFirstByOrderByBranchIdAsc().orElseThrow(() -> new IllegalStateException("등록된 지점이 없습니다."));
    }

    private int gramsFor(String name) {
        String n = name.replace(" ", "");
        if (n.contains("하프갤론")) return 1237; if (n.contains("패밀리")) return 989;
        if (n.contains("쿼터")) return 643; if (n.contains("파인트")) return 336;
        if (n.contains("더블레귤러")) return 230; if (n.contains("더블주니어")) return 150;
        if (n.contains("싱글킹")) return 145; return 115;
    }

    public record CreateOrderRequest(Long branchId, String kioskCode, String orderType, String paymentMethod,
                                     Integer discountAmount, List<CreateOrderItemRequest> items) {}
    public record CreateOrderItemRequest(Long productId, Integer quantity, List<Long> flavorIds) {}
    public record CreateOrderResponse(Long orderId, String orderNumber, Integer waitingNumber, Integer finalAmount) {}
    public record OrderResponse(Long orderId, Integer waitingNumber, String orderNumber, String orderType, String status,
                                String menu, Integer finalAmount, LocalDateTime createdAt, long elapsedMinutes) {}
    public record InventoryResponse(Long inventoryId, Long flavorId, String flavorName, String imageUrl,
                                    Integer remainingG, String status, String requestStatus,
                                    LocalDateTime expectedArrivalAt, LocalDateTime updatedAt) {}
}
