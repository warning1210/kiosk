package com.kiosk.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kiosk.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 1. 대시보드 상태별 카운트 용도
    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus = :status AND o.createdAt BETWEEN :start AND :end")
    long countByBranchIdAndOrderStatusAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("status") OrderStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // 2. 대시보드 오늘 매출 용도 (해당 지점의 오늘 결제완료/제조중/완료된 주문)
    @Query("SELECT o FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus IN :statuses AND o.createdAt BETWEEN :start AND :end")
    List<Order> findByBranchIdAndOrderStatusInAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // 3. 지점 주문 리스트 조회 용도
    @Query("SELECT o FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses);
}
