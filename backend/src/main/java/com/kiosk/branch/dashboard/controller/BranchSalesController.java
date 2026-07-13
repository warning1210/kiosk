package com.kiosk.branch.dashboard.controller;

import com.kiosk.branch.dashboard.service.BranchSalesService;
import com.kiosk.global.security.BranchAccessService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/sales")
@RequiredArgsConstructor
public class BranchSalesController {

    private final BranchSalesService branchSalesService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public Map<String, Object> getStatistics(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return branchSalesService.getStatistics(branchAccessService.requireBranchId(authorization));
    }
}
