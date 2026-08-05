package com.kiosk.domain.inventory;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BranchInventoryRepository {
    Optional<BranchInventory> findByBranch_BranchIdAndFlavor_FlavorId(Long branchId, Long flavorId);
    List<BranchInventory> findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(Long branchId);
    Optional<BranchInventory> findById(Long id);
    List<BranchInventory> findAll();
    int insert(BranchInventory inventory);
    int update(BranchInventory inventory);
    default BranchInventory save(BranchInventory inventory) {
        if (inventory.getBranchInventoryId() == null) insert(inventory); else update(inventory);
        return inventory;
    }
    default List<BranchInventory> saveAll(List<BranchInventory> values) { values.forEach(this::save); return values; }
}
