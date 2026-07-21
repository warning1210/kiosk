package com.kiosk.hq.dashboard.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.dashboard.dto.HqDashboardStatsResponse;
import com.kiosk.hq.dashboard.service.HqDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/dashboard")
@RequiredArgsConstructor
public class HqDashboardController {

    private final HqDashboardService hqDashboardService;
    private final HqAccessService hqAccessService;

    @GetMapping("/stats")
    public HqDashboardStatsResponse stats(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqDashboardService.getStats();
    }
}
