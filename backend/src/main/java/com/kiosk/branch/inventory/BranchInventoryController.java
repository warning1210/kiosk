package com.kiosk.branch.inventory;

import com.kiosk.branch.inventory.dto.BranchInventoryItemResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.global.response.ApiResponse;
import com.kiosk.global.security.CurrentAdmin;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BranchInventoryController {

    private final BranchInventoryService branchInventoryService;

    public BranchInventoryController(BranchInventoryService branchInventoryService) {
        this.branchInventoryService = branchInventoryService;
    }

    @GetMapping("/api/branch/inventory")
    public ApiResponse<List<BranchInventoryItemResponse>> getInventory(
            @CurrentAdmin Admin admin,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(branchInventoryService.getInventory(admin, categoryId, keyword));
    }
}
