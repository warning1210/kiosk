package com.kiosk.branch.dashboard.controller;

import com.kiosk.branch.dashboard.service.BranchEventPublisher;
import com.kiosk.global.security.BranchAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 브라우저 기본 EventSource는 커스텀 헤더(Authorization)를 못 보내므로, 이 스트림 엔드포인트만
// 예외적으로 토큰을 쿼리 파라미터로 받는다. 다른 /api/branch/** 는 전부 헤더 방식 그대로다.
@RestController
@RequestMapping("/api/branch/events")
@RequiredArgsConstructor
public class BranchEventStreamController {

    private final BranchAccessService branchAccessService;
    private final BranchEventPublisher branchEventPublisher;

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Long branchId = branchAccessService.requireBranchId("Bearer " + token);
        return branchEventPublisher.subscribe(branchId);
    }
}
