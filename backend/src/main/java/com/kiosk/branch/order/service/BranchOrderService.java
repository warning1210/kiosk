package com.kiosk.branch.order.service;

import com.kiosk.branch.order.dto.BranchOrderListResponse;
import com.kiosk.branch.order.dto.OrderStatusUpdateRequest;
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
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        order.setOrderStatus(request.getStatus());
        // 취소 사유 저장 등의 추가 로직이 필요하면 여기에 작성
    }
}
