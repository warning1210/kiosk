package com.kiosk.domain.order;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 지점 대시보드 상태별 카운트
    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus = :status AND o.createdAt BETWEEN :start AND :end")
    long countByBranchIdAndOrderStatusAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("status") OrderStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 지점 대시보드 오늘 매출 집계용
    @Query("SELECT o FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus IN :statuses AND o.createdAt BETWEEN :start AND :end")
    List<Order> findByBranchIdAndOrderStatusInAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 지점 주문 목록 조회 (오래된 순)
    @Query("SELECT o FROM Order o WHERE o.branch.branchId = :branchId AND o.orderStatus IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses);
}
