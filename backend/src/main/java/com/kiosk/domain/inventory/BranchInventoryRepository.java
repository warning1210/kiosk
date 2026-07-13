package com.kiosk.domain.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
    Optional<BranchInventory> findByBranch_BranchIdAndFlavor_FlavorId(Long branchId, Long flavorId);
    List<BranchInventory> findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(Long branchId);
}
