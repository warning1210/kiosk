package com.kiosk.domain.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {

    Optional<BranchInventory> findByBranch_BranchIdAndFlavor_FlavorId(Long branchId, Long flavorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bi FROM BranchInventory bi
            WHERE bi.branch.branchId = :branchId
              AND bi.flavor.flavorId = :flavorId
            """)
    Optional<BranchInventory> findByBranchAndFlavorForUpdate(
            @Param("branchId") Long branchId,
            @Param("flavorId") Long flavorId);

    @Query("""
            SELECT bi FROM BranchInventory bi
            JOIN FETCH bi.flavor f
            LEFT JOIN f.category c
            WHERE bi.branch.branchId = :branchId
              AND (:categoryId IS NULL OR c.categoryId = :categoryId)
              AND (:keyword IS NULL OR f.flavorName LIKE CONCAT('%', :keyword, '%'))
            ORDER BY (bi.currentQuantity - bi.safetyQuantity) ASC, f.flavorName ASC
            """)
    List<BranchInventory> search(@Param("branchId") Long branchId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword);
}
