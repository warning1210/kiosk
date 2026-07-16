package com.kiosk.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
}
