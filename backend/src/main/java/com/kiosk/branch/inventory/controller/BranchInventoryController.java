package com.kiosk.branch.inventory.controller;

import com.kiosk.branch.inventory.dto.InventoryAdjustRequest;
import com.kiosk.branch.inventory.dto.InventoryResponse;
import com.kiosk.branch.inventory.service.BranchInventoryService;
import com.kiosk.global.security.BranchAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/inventory")
@RequiredArgsConstructor
public class BranchInventoryController {

    private final BranchInventoryService branchInventoryService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public List<InventoryResponse> getInventory(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return branchInventoryService.getInventory(branchAccessService.requireBranchId(authorization));
    }

    @PatchMapping("/{inventoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjust(@PathVariable Long inventoryId, @RequestBody InventoryAdjustRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        branchInventoryService.adjustInventory(inventoryId, request, branchAccessService.requireBranchId(authorization));
    }

    @PostMapping("/{inventoryId}/receive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(@PathVariable Long inventoryId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        branchInventoryService.receiveInventory(inventoryId, branchAccessService.requireBranchId(authorization));
    }
}
