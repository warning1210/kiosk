package com.kiosk.branch.order.service;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.order.OrderItemRepository;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemFlavorRepository orderItemFlavorRepository;

    @Transactional(readOnly = true)
    public List<BranchOrderListResponse> getBranchOrders(Long branchId) {
        // PAID, MAKING, COMPLETED 주문을 생성시간 오름차순(오래된 순)으로 조회
        List<Order> orders = orderRepository.findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(
                branchId, 
                List.of(OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.COMPLETED)
        );

        LocalDateTime now = LocalDateTime.now();

        return orders.stream().map(order -> {
            long elapsedMinutes = Duration.between(order.getCreatedAt(), now).toMinutes();
            
            // 상세 메뉴 및 맛 요약 문자열 생성
            List<OrderItem> items = orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(order.getOrderId());
            List<String> itemDescriptions = new ArrayList<>();
            for (OrderItem item : items) {
                List<OrderItemFlavor> flavors = orderItemFlavorRepository.findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(item.getOrderItemId());
                String flavorStr = "";
                if (!flavors.isEmpty()) {
                    flavorStr = "(" + flavors.stream()
                            .map(OrderItemFlavor::getFlavorNameSnapshot)
                            .collect(Collectors.joining(", ")) + ")";
                }
                itemDescriptions.add(item.getProductNameSnapshot() + flavorStr);
            }
            String menuSummary = itemDescriptions.isEmpty() ? "주문 내역 없음" : String.join(", ", itemDescriptions);

            return BranchOrderListResponse.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .waitingNumber(order.getWaitingNumber())
                    .elapsedMinutes(elapsedMinutes)
                    .orderType(order.getOrderType())
                    .menuSummary(menuSummary)
                    .status(order.getOrderStatus())
                    .createdAt(order.getCreatedAt())
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
            if (request.getCancelReason() == null || request.getCancelReason().isBlank()) {
                throw new IllegalArgumentException("취소 사유를 입력해주세요.");
            }
            order.setCancellationReason(request.getCancelReason());
        }
        order.setOrderStatus(request.getStatus());
    }
}
