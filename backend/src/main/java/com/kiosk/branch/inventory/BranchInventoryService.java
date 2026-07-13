package com.kiosk.branch.inventory;

import com.kiosk.branch.inventory.dto.BranchInventoryItemResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import com.kiosk.global.security.ActorGuard;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BranchInventoryService {

    private final BranchInventoryRepository branchInventoryRepository;

    public BranchInventoryService(BranchInventoryRepository branchInventoryRepository) {
        this.branchInventoryRepository = branchInventoryRepository;
    }

    public List<BranchInventoryItemResponse> getInventory(Admin admin, Long categoryId, String keyword) {
        Branch branch = ActorGuard.requireBranchOf(admin);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return branchInventoryRepository.search(branch.getBranchId(), categoryId, normalizedKeyword).stream()
                .map(BranchInventoryItemResponse::from)
                .toList();
    }
}
