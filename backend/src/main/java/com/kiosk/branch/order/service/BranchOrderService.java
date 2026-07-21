package com.kiosk.branch.order.service;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
import com.kiosk.branch.order.dto.BranchOrderDetailItemResponse;
import com.kiosk.branch.order.dto.BranchOrderDetailPaymentResponse;
import com.kiosk.branch.order.dto.BranchOrderDetailResponse;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentRepository;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchOrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemFlavorRepository orderItemFlavorRepository;

    @Transactional(readOnly = true)
    public List<BranchOrderListResponse> getBranchOrders(Long branchId, java.time.LocalDate date) {
        // PAID, MAKING, READY, COMPLETED, CANCELLED 주문을 생성시간 오름차순(오래된 순)으로 조회
        List<OrderStatus> statuses = List.of(OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.READY,
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

        return orders.stream().map(order -> {
            long elapsedMinutes = Duration.between(order.getCreatedAt(), now).toMinutes();

            // 메뉴 요약 문자열 생성 로직 (ex: 바닐라 파인트 외 2건)
            String menuSummary = "주문 내역 없음";
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                OrderItem firstItem = order.getOrderItems().get(0);
                if (order.getOrderItems().size() > 1) {
                    menuSummary = firstItem.getProductNameSnapshot() + " 외 " + (order.getOrderItems().size() - 1) + "건";
                } else {
                    menuSummary = firstItem.getProductNameSnapshot();
                }
            }

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
        order.setOrderStatus(request.getStatus());
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