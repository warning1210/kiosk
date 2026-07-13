package com.kiosk.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findTop100ByBranch_BranchIdOrderByCreatedAtDesc(Long branchId);
    long countByBranch_BranchIdAndCreatedAtAfter(Long branchId, LocalDateTime after);
}
