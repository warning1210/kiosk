package com.kiosk.branch.status.controller;

import com.kiosk.branch.status.dto.BranchStatusResponse;
import com.kiosk.branch.status.dto.BranchStatusUpdateRequest;
import com.kiosk.branch.status.service.BranchStatusService;
import com.kiosk.global.security.BranchAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branch/status")
public class BranchStatusController {

    private final BranchStatusService branchStatusService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public ResponseEntity<BranchStatusResponse> getStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        return ResponseEntity.ok(branchStatusService.getStatus(branchId));
    }

    @PatchMapping
    public ResponseEntity<BranchStatusResponse> updateStatus(
            @RequestBody BranchStatusUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long branchId = branchAccessService.requireBranchId(authorization);
        return ResponseEntity.ok(branchStatusService.updateStatus(branchId, request));
    }
}
