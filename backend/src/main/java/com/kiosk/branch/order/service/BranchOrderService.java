package com.kiosk.branch.order.service;

import com.kiosk.branch.dashboard.service.BranchEventPublisher;
import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
import com.kiosk.branch.order.dto.BranchOrderDetailItemResponse;
import com.kiosk.branch.order.dto.BranchOrderDetailPaymentResponse;
import com.kiosk.branch.order.dto.BranchOrderDetailResponse;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.order.OrderItemRepository;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentMethod;
import com.kiosk.domain.payment.PaymentRepository;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.kiosk.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchOrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemFlavorRepository orderItemFlavorRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentService paymentService;
    private final BranchEventPublisher branchEventPublisher;

    @Transactional(readOnly = true)
    public List<BranchOrderListResponse> getBranchOrders(
            Long branchId, java.time.LocalDate date, boolean activeOnly) {

        List<OrderStatus> statuses = activeOnly
                ? List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.READY)
                : List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.READY,
                        OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        List<Order> orders;

        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            orders = orderRepository.findByBranchIdAndOrderStatusInAndCreatedAtBetween(branchId, statuses, start, end);
        } else {
            orders = orderRepository.findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(branchId, statuses);
        }

        LocalDateTime now = LocalDateTime.now();

        // 주문마다 결제를 따로 조회하면(N+1) Payment의 order 매핑이 서브쿼리로 Order+OrderItem을
        // 통째로 재조회해서 주문 수만큼 증폭돼 심각하게 느려진다 - 결제수단만 한 번에 벌크 조회한다.
        List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();
        Map<Long, PaymentMethod> paymentMethodByOrderId = orderIds.isEmpty() ? Map.of()
                : paymentRepository.findMethodsByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(p -> p.getOrder().getOrderId(), Payment::getPaymentMethod));

        // 이 목록 조회(listMap)는 orderItems를 안 채우므로(같은 이유의 N+1 방지) 메뉴 요약에 쓸
        // 주문상품도 한 번에 벌크 조회해 주문ID별로 묶어둔다.
        Map<Long, List<OrderItem>> orderItemsByOrderId = orderIds.isEmpty() ? Map.of()
                : orderItemRepository.findByOrder_OrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getOrder().getOrderId()));

        return orders.stream()
                // PENDING_PAYMENT 상태는 현금 주문만 주문관리에 미리 보여준다.
                // 카드/QR 주문은 결제가 승인되어 PAID가 된 뒤 나타나므로 기존처럼 곧바로
                // "제조 시작" 버튼이 보이고, 카운터 결제 확인 단계는 거치지 않는다.
                .filter(order -> order.getOrderStatus() != OrderStatus.PENDING_PAYMENT
                        || paymentMethodByOrderId.get(order.getOrderId()) == PaymentMethod.CASH)
                .map(order -> {
            long elapsedMinutes = Duration.between(order.getCreatedAt(), now).toMinutes();

            // 메뉴 요약 문자열 생성 로직 (ex: 바닐라 파인트 외 2건)
            List<OrderItem> items = orderItemsByOrderId.getOrDefault(order.getOrderId(), List.of());
            String menuSummary = "주문 내역 없음";
            if (!items.isEmpty()) {
                OrderItem firstItem = items.get(0);
                menuSummary = items.size() > 1
                        ? firstItem.getProductNameSnapshot() + " 외 " + (items.size() - 1) + "건"
                        : firstItem.getProductNameSnapshot();
            }

            // 결제수단을 목록 DTO에 포함하면 프론트가 현금 주문을 별도 배지로 표시할 수 있다.
            PaymentMethod paymentMethod = paymentMethodByOrderId.get(order.getOrderId());
            return BranchOrderListResponse.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .waitingNumber(order.getWaitingNumber())
                    .elapsedMinutes(elapsedMinutes)
                    .orderType(order.getOrderType())
                    .menuSummary(menuSummary)
                    .status(order.getOrderStatus())
                    .createdAt(order.getCreatedAt())
                    .finalAmount(order.getFinalAmount())
                    .paymentMethod(paymentMethod != null ? paymentMethod.name() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long branchId, Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (!order.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("다른 지점의 주문은 변경할 수 없습니다.");
        }

        if (request.getStatus() == OrderStatus.CANCELLED) {
            order.setCancellationReason(request.getCancelReason());
        }
        if (request.getStatus() == OrderStatus.COMPLETED) {
            order.setOrderCompletedAt(LocalDateTime.now());
        }
        order.setOrderStatus(request.getStatus());
        // MyBatis는 JPA와 달리 트랜잭션 커밋 시 변경분을 자동 flush하지 않는다 - 명시적으로 저장해야
        // DB에 실제로 반영된다 (이게 빠져 있으면 상태 변경 버튼을 눌러도 DB는 그대로였다).
        orderRepository.update(order);

        branchEventPublisher.publish(branchId, "order");
    }

    /**
     * 지점 직원이 현금을 받은 시점에만 결제를 확정한다.
     * 먼저 소속 지점을 검증한 후 결제 서비스의 공통 확정 로직을 호출한다.
     */
    @Transactional
    public void confirmCashPayment(Long branchId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (!order.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("다른 지점의 주문은 결제할 수 없습니다.");
        }
        paymentService.confirmCash(orderId);
    }

    @Transactional(readOnly = true)
    public BranchOrderDetailResponse getBranchOrderDetail(Long branchId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (!order.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(orderId).orElse(null);

        List<BranchOrderDetailItemResponse> itemResponses = order.getOrderItems().stream().map(item -> {
            List<OrderItemFlavor> flavors = orderItemFlavorRepository
                    .findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(item.getOrderItemId());
            List<String> options = flavors.stream()
                    .map(OrderItemFlavor::getFlavorNameSnapshot)
                    .collect(Collectors.toList());

            if (item.getContainerType() != null && item.getContainerType() != com.kiosk.domain.order.ContainerType.NONE) {
                switch (item.getContainerType()) {
                    case CUP: options.add("용기: 컵"); break;
                    case CONE: options.add("용기: 콘"); break;
                    case WAFFLE_CONE: options.add("용기: 와플콘"); break;
                    default: break;
                }
            }
            if (item.getDryIceMinutes() != null && item.getDryIceMinutes() > 0) {
                options.add("드라이아이스: " + item.getDryIceMinutes() + "분");
            }
            if (item.getSpoonCount() != null && item.getSpoonCount() > 0) {
                options.add("스푼: " + item.getSpoonCount() + "개");
            }

            return BranchOrderDetailItemResponse.builder()
                    .productName(item.getProductNameSnapshot())
                    .unitPrice(item.getUnitPriceSnapshot())
                    .quantity(item.getQuantity())
                    .itemTotal(item.getItemTotal())
                    .options(options)
                    .build();
        }).collect(Collectors.toList());

        BranchOrderDetailPaymentResponse paymentResponse = null;
        if (payment != null) {
            paymentResponse = BranchOrderDetailPaymentResponse.builder()
                    .requestedAmount(payment.getRequestedAmount())
                    .paidAmount(payment.getPaidAmount())
                    .paymentMethod(payment.getPaymentMethod().name())
                    .approvalNumber(payment.getApprovalNumber())
                    .paidAt(payment.getPaidAt())
                    .paymentStatus(payment.getPaymentStatus().name())
                    .installment("일시불") // 임시값
                    .cardNumber("등록된 카드") // 임시값
                    .build();
        }

        return BranchOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .waitingNumber(order.getWaitingNumber())
                .orderType(order.getOrderType())
                .status(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .amountBeforeDiscount(order.getAmountBeforeDiscount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .payment(paymentResponse)
                .items(itemResponses)
                .build();
    }
    @Transactional(readOnly = true)
    public List<String> getAvailableOrderDates(Long branchId) {
        List<LocalDateTime> createdAts = orderRepository.findCreatedAtByBranchId(branchId);
        return createdAts.stream()
                .map(dateTime -> dateTime.toLocalDate().toString())
                .distinct()
                .collect(Collectors.toList());
    }
}
