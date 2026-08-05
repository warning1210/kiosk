package com.kiosk.branch.auth.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.branch.auth.dto.FirebaseSessionRequest;
import com.kiosk.branch.auth.dto.JoinRequest;
import com.kiosk.branch.auth.dto.LoginIdentityResponse;
import com.kiosk.branch.auth.dto.LoginResponse;
import com.kiosk.branch.auth.service.BranchAuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;


@Validated
@RestController
@RequestMapping("/api/branch-auth")
@RequiredArgsConstructor
public class BranchAuthController {

    private final BranchAuthService branchAuthService;

    @GetMapping("/invites/{token}")
    public ApplicationResponse invite(
            @PathVariable
            @Pattern(
                regexp = "^[a-zA-Z0-9\\-]{1,100}$",
                message = "유효하지 않은 초대 토큰입니다."
            )
            String token
    ) {
        return branchAuthService.invite(token);
    }


    @PostMapping("/join")
    public LoginResponse join(
            @Valid @RequestBody JoinRequest request
    ) {
        return branchAuthService.join(request);
    }


    @GetMapping("/login-identity/{loginId}")
    public LoginIdentityResponse loginIdentity(
            @PathVariable String loginId
    ) {
        return branchAuthService.loginIdentity(loginId);
    }


    @PostMapping("/firebase-session")
    public LoginResponse firebaseSession(
            @RequestBody FirebaseSessionRequest request
    ) {
        return branchAuthService.firebaseSession(request);
    }
}