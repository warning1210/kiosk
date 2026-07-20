package com.kiosk.hq.auth.controller;

import com.kiosk.hq.auth.dto.HqLoginRequest;
import com.kiosk.hq.auth.dto.HqLoginResponse;
import com.kiosk.hq.auth.service.HqAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq-auth")
@RequiredArgsConstructor
public class HqAuthController {

    private final HqAuthService hqAuthService;

    @PostMapping("/login")
    public HqLoginResponse login(@RequestBody HqLoginRequest request) {
        return hqAuthService.login(request);
    }
}
