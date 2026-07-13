package com.kiosk.branch.dashboard.controller;

import com.kiosk.branch.dashboard.dto.BranchDashboardSummaryDto;
import com.kiosk.branch.dashboard.service.BranchDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branch/{branchId}/dashboard")
public class BranchDashboardController {

    private final BranchDashboardService branchDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<BranchDashboardSummaryDto> getDashboardSummary(@PathVariable Long branchId) {
        BranchDashboardSummaryDto summary = branchDashboardService.getDashboardSummary(branchId);
        return ResponseEntity.ok(summary);
    }
}
