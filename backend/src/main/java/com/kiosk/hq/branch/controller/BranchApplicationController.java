package com.kiosk.hq.branch.controller;

import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.hq.branch.service.BranchApplicationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/branch-applications")
@RequiredArgsConstructor
public class BranchApplicationController {

    private final BranchApplicationService branchApplicationService;

    @GetMapping
    public List<ApplicationResponse> list(@RequestHeader(value = "Origin", required = false) String origin) {
        return branchApplicationService.list(origin);
    }

    @PostMapping("/{id}/approve")
    public ApplicationResponse approve(@PathVariable Long id) {
        return branchApplicationService.approve(id);
    }

    @DeleteMapping("/{id}/account")
    public ApplicationResponse deleteAccount(@PathVariable Long id) {
        return branchApplicationService.deleteAccount(id);
    }
}
