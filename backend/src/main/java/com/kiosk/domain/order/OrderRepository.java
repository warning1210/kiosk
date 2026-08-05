package com.kiosk.domain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderRepository {
    Order selectById(Long id);
    default Optional<Order> findById(Long id) { return Optional.ofNullable(selectById(id)); }
    long countByBranchIdAndOrderStatusAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("status") OrderStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Order> findByBranchIdAndOrderStatusInAndCreatedAtBetween(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Order> findByBranchIdAndOrderStatusInOrderByCreatedAtAsc(@Param("branchId") Long branchId, @Param("statuses") List<OrderStatus> statuses);
    List<LocalDateTime> findCreatedAtByBranchId(Long branchId);
    List<Order> findByOrderStatusInAndCreatedAtBefore(@Param("statuses") List<OrderStatus> statuses, @Param("cutoff") LocalDateTime cutoff);
    Integer findMaxCashWaitingNumber(@Param("branchId") Long branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Integer findMaxQrWaitingNumber(@Param("branchId") Long branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    int insert(Order value);
    int update(Order value);
    default Order save(Order value) { if (value.getOrderId() == null) insert(value); else update(value); return value; }
    default void saveAll(Iterable<Order> values) { values.forEach(this::save); }
}
