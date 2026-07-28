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

    // 지점의 모든 주문 생성일시 조회
    @Query("SELECT o.createdAt FROM Order o WHERE o.branch.branchId = :branchId")
    List<LocalDateTime> findCreatedAtByBranchId(@Param("branchId") Long branchId);

    // 자정 이전의 미처리 주문 목록 조회 (자동 취소용)
    @Query("SELECT o FROM Order o WHERE o.orderStatus IN :statuses AND o.createdAt < :cutoff")
    List<Order> findByOrderStatusInAndCreatedAtBefore(@Param("statuses") List<OrderStatus> statuses, @Param("cutoff") LocalDateTime cutoff);

    /**
     * 오늘 해당 지점에서 만들어진 현금 주문 중 가장 큰 대기번호를 찾는다.
     * 현금 주문번호를 카드/QR 주문번호와 분리해 매일 1번부터 발급하기 위한 조회다.
     */
    @Query("""
            SELECT MAX(o.waitingNumber)
              FROM Order o
              JOIN Payment p ON p.order = o
             WHERE o.branch.branchId = :branchId
               AND p.paymentMethod = com.kiosk.domain.payment.PaymentMethod.CASH
               AND o.createdAt BETWEEN :start AND :end
            """)
    Integer findMaxCashWaitingNumber(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 오늘 해당 지점의 카드/QR 주문 중 가장 큰 번호를 조회한다.
     * 카드 주문은 현금 주문과 번호가 섞이지 않도록 300번부터 발급한다.
     */
    @Query("""
            SELECT MAX(o.waitingNumber)
              FROM Order o
              JOIN Payment p ON p.order = o
             WHERE o.branch.branchId = :branchId
               AND p.paymentMethod = com.kiosk.domain.payment.PaymentMethod.QR
               AND o.createdAt BETWEEN :start AND :end
            """)
    Integer findMaxQrWaitingNumber(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
