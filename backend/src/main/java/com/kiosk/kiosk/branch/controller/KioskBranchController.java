package com.kiosk.kiosk.branch.controller;

import com.kiosk.kiosk.branch.dto.BranchStatusResponse;
import com.kiosk.kiosk.branch.service.KioskBranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KioskBranchController {

    private final KioskBranchService kioskBranchService;

    @GetMapping("/branch-status")
    public BranchStatusResponse getBranchStatus(@RequestParam Long branchId) {
        return kioskBranchService.getStatus(branchId);
    }
}
