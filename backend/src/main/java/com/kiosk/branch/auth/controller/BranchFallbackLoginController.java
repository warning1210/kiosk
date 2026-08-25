package com.kiosk.branch.auth.controller;

import com.kiosk.branch.auth.dto.DbLoginRequest;
import com.kiosk.branch.auth.dto.DbLoginResponse;
import com.kiosk.branch.auth.service.BranchFallbackLoginService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Firebase 장애 시의 폴백 로그인 전용 컨트롤러 - BranchAuthController(Firebase 로그인)와 완전히 분리된 파일이다.
@RestController
@RequestMapping("/api/branch-auth")
@RequiredArgsConstructor
public class BranchFallbackLoginController {

    private final BranchFallbackLoginService branchFallbackLoginService;

    @PostMapping("/db-login")
    public DbLoginResponse dbLogin(@RequestBody DbLoginRequest request, HttpServletResponse response) {
        return branchFallbackLoginService.login(request, response);
    }
}
