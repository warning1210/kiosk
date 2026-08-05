package com.kiosk.domain.product;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BranchProductRepository {
    List<BranchProduct> findByBranch_BranchId(Long branchId);
    BranchProduct selectByBranchAndProduct(@Param("branchId") Long branchId, @Param("productId") Long productId);
    default Optional<BranchProduct> findByBranch_BranchIdAndProduct_ProductId(Long branchId, Long productId) { return Optional.ofNullable(selectByBranchAndProduct(branchId, productId)); }
    int insert(BranchProduct value);
    int update(BranchProduct value);
    default BranchProduct save(BranchProduct value) { if (value.getBranchProductId() == null) insert(value); else update(value); return value; }
}
