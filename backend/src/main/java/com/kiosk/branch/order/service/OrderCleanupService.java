package com.kiosk.branch.order.service;

import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCleanupService {

    private final OrderRepository orderRepository;

    /**
     * 자정부터 생성된 주문이 아니면서, 아직 처리가 완료되지 않은(PAID, MAKING, READY) 주문들을
     * 주기적으로 찾아 자동으로 취소 상태로 변경합니다.
     * 여기서는 매 1분마다 실행되도록 설정합니다. (실제 운영 환경에서는 밤 12시 정각 등에 실행 가능)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelOldOrders() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<OrderStatus> activeStatuses = List.of(OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.READY);

        List<Order> oldOrders = orderRepository.findByOrderStatusInAndCreatedAtBefore(activeStatuses, startOfToday);

        if (!oldOrders.isEmpty()) {
            for (Order order : oldOrders) {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("주문 처리일 경과 자동 취소 처리");
                log.info("자동 취소 처리된 주문 ID: {}", order.getOrderId());
            }
            orderRepository.saveAll(oldOrders);
        }
    }
}
