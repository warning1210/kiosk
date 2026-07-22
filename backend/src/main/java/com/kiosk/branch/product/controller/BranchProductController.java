package com.kiosk.branch.product.controller;

import com.kiosk.branch.product.dto.BranchProductResponse;
import com.kiosk.branch.product.dto.BranchProductVisibilityRequest;
import com.kiosk.branch.product.service.BranchProductService;
import com.kiosk.global.security.BranchAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/products")
@RequiredArgsConstructor
public class BranchProductController {

    private final BranchProductService branchProductService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public List<BranchProductResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        return branchProductService.list(branchId);
    }

    @PatchMapping("/{productId}/visibility")
    public BranchProductResponse toggleVisibility(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long productId,
            @RequestBody BranchProductVisibilityRequest request) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        return branchProductService.toggleVisibility(branchId, productId, request.isVisible());
    }
}
