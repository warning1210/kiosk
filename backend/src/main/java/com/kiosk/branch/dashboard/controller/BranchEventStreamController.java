package com.kiosk.branch.dashboard.controller;

import com.kiosk.branch.dashboard.service.BranchEventPublisher;
import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.AdminTokenService;
import com.kiosk.global.security.BranchAccessService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 브라우저 기본 EventSource는 커스텀 헤더(Authorization)를 못 보내므로, 이 스트림 엔드포인트만
// 예외적으로 토큰을 쿼리 파라미터로 받는다. 다른 /api/branch/** 는 전부 헤더 방식 그대로다.
//
// 쿼리파라미터는 액세스 로그(%r)에 그대로 남기 때문에, 12시간짜리 풀 권한 로그인 토큰을 그대로
// 쓰면 로그 유출 시 그 지점 계정으로 뭐든 할 수 있는 토큰이 같이 새어나간다. 그래서 /stream을
// 열기 직전에 헤더 인증(안전)으로 1분짜리 전용 티켓을 먼저 받고, 그 티켓만 쿼리파라미터로 쓴다 -
// 새어나가도 만료가 빠르고 스트림 구독 외에는 아무 권한이 없다.
@RestController
@RequestMapping("/api/branch/events")
@RequiredArgsConstructor
public class BranchEventStreamController {

    private final BranchAccessService branchAccessService;
    private final BranchEventPublisher branchEventPublisher;
    private final AdminTokenService adminTokenService;

    @GetMapping("/stream-ticket")
    public Map<String, String> issueStreamTicket(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return Map.of("ticket", adminTokenService.issueStreamTicket(admin.getAdminId()));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Long branchId = branchAccessService.requireBranchIdForStream(token);
        return branchEventPublisher.subscribe(branchId);
    }
}
