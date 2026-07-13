package com.kiosk.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
    Optional<BranchInventory> findByBranch_BranchIdAndFlavor_FlavorId(Long branchId, Long flavorId);
    List<BranchInventory> findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(Long branchId);

    @Query("select bi.flavor.flavorId from BranchInventory bi where bi.branch.branchId = :branchId and bi.currentQuantity > 0 and bi.isKioskVisible = true")
    List<Long> findKioskVisibleFlavorIds(@Param("branchId") Long branchId);
}
