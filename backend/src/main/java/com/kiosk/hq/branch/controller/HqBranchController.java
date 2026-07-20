package com.kiosk.hq.branch.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.branch.dto.HqBranchStatusResponse;
import com.kiosk.hq.branch.service.HqBranchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/branches")
@RequiredArgsConstructor
public class HqBranchController {

    private final HqBranchService hqBranchService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqBranchStatusResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqBranchService.listWithSales();
    }
}
