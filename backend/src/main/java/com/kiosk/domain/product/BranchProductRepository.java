package com.kiosk.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchProductRepository extends JpaRepository<BranchProduct, Long> {
    List<BranchProduct> findByBranch_BranchId(Long branchId);
    Optional<BranchProduct> findByBranch_BranchIdAndProduct_ProductId(Long branchId, Long productId);
}
