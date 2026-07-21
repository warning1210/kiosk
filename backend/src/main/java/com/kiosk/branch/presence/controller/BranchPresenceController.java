package com.kiosk.branch.presence.controller;

import com.kiosk.global.security.BranchAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 지점 관리 화면이 열려 있는 동안 본점에 현재 온라인 상태를 알리는 API다.
@RestController
@RequestMapping("/api/branch/presence")
@RequiredArgsConstructor
public class BranchPresenceController {

    private final BranchAccessService branchAccessService;

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 인증 과정에서 해당 지점의 마지막 접속 시각이 현재 시각으로 갱신된다.
        branchAccessService.requireAdmin(authorization);
    }
}
