package com.kiosk.kiosk.order.service;

import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.kiosk.order.dto.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KioskOrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Long createOrder(OrderCreateRequest request) {
        // 1. Vue 프론트엔드에서 쿠키로 들고 있던 장바구니 금액 검증 (여기서는 생략 및 총액 단순 계산)
        int totalAmount = 0;
        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            totalAmount += (item.getUnitPriceSnapshot() * item.getQuantity());
        }
        
        int finalAmount = totalAmount - (request.getUsedPoints() != null ? request.getUsedPoints() : 0);

        // 2. Order 엔티티 생성 (현재 결제가 확정되었다고 가정하여 PAID 상태로 저장)
        Order order = Order.builder()
                .branch(com.kiosk.domain.branch.Branch.builder().branchId(request.getBranchId()).build())
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .waitingNumber((int) (Math.random() * 100) + 1) // 임시 대기번호
                .orderType(request.getOrderType())
                .orderStatus(OrderStatus.PAID)
                .amountBeforeDiscount(totalAmount)
                .discountAmount(request.getUsedPoints() != null ? request.getUsedPoints() : 0)
                .finalAmount(finalAmount)
                .usedPoints(request.getUsedPoints() != null ? request.getUsedPoints() : 0)
                .earnedPoints(0) // 포인트 적립 로직 임시 생략
                .language(com.kiosk.domain.common.Language.ko)
                .isEasyMode(false)
                .build();

        // 3. OrderItem 및 Flavor 엔티티 매핑
        for (OrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(com.kiosk.domain.product.Product.builder().productId(itemReq.getProductId()).build())
                    .productNameSnapshot(itemReq.getProductNameSnapshot())
                    .unitPriceSnapshot(itemReq.getUnitPriceSnapshot())
                    .quantity(itemReq.getQuantity())
                    .itemTotal(itemReq.getUnitPriceSnapshot() * itemReq.getQuantity())
                    .spoonCount(0)
                    .build();

            int selectionOrder = 1;
            for (OrderCreateRequest.FlavorRequest flavorReq : itemReq.getFlavors()) {
                OrderItemFlavor flavor = OrderItemFlavor.builder()
                        .orderItem(orderItem)
                        .flavor(com.kiosk.domain.flavor.Flavor.builder().flavorId(flavorReq.getFlavorId()).build())
                        .flavorNameSnapshot(flavorReq.getFlavorNameSnapshot())
                        .selectionOrder(selectionOrder++)
                        .quantity(1)
                        .build();
                orderItem.getOrderItemFlavors().add(flavor);
            }
            order.getOrderItems().add(orderItem);
        }

        // 4. DB에 저장
        Order savedOrder = orderRepository.save(order);
        return savedOrder.getOrderId();
    }
}
