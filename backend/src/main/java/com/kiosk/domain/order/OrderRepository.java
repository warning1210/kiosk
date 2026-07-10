package com.kiosk.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiosk.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 1. 대시보드 상태별 카운트 용도
    long countByBranchIdAndOrderStatusAndCreatedAtBetween(Long branchId, OrderStatus status, LocalDateTime start, LocalDateTime end);
    
    // 2. 대시보드 오늘 매출 용도 (해당 지점의 오늘 결제완료/제조중/완료된 주문)
    List<Order> findByBranchIdAndOrderStatusInAndCreatedAtBetween(Long branchId, List<OrderStatus> statuses, LocalDateTime start, LocalDateTime end);
    
    // 3. 지점 주문 리스트 조회 용도
    List<Order> findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(Long branchId, List<OrderStatus> statuses);
}
