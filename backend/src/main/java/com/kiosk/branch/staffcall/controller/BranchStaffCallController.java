package com.kiosk.branch.staffcall.controller;

import com.kiosk.branch.staffcall.dto.StaffCallStatusResponse;
import com.kiosk.branch.staffcall.service.BranchStaffCallService;
import com.kiosk.global.security.BranchAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branch/staff-call")
public class BranchStaffCallController {

    private final BranchStaffCallService branchStaffCallService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public StaffCallStatusResponse getStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        return branchStaffCallService.getStatus(branchId);
    }

    @DeleteMapping
    public void acknowledge(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        branchStaffCallService.acknowledge(branchId);
    }
}
