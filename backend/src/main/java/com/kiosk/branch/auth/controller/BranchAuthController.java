package com.kiosk.branch.auth.controller;

import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.branch.auth.dto.ApplyRequest;
import com.kiosk.branch.auth.dto.FirebaseSessionRequest;
import com.kiosk.branch.auth.dto.JoinRequest;
import com.kiosk.branch.auth.dto.LoginIdentityResponse;
import com.kiosk.branch.auth.dto.LoginResponse;
import com.kiosk.branch.auth.service.BranchAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch-auth")
@RequiredArgsConstructor
public class BranchAuthController {

    private final BranchAuthService branchAuthService;

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@RequestBody ApplyRequest request) {
        return branchAuthService.apply(request);
    }

    @GetMapping("/invites/{token}")
    public ApplicationResponse invite(@PathVariable String token) {
        return branchAuthService.invite(token);
    }

    @PostMapping("/join")
    public LoginResponse join(@RequestBody JoinRequest request) {
        return branchAuthService.join(request);
    }

    @GetMapping("/login-identity/{loginId}")
    public LoginIdentityResponse loginIdentity(@PathVariable String loginId) {
        return branchAuthService.loginIdentity(loginId);
    }

    @PostMapping("/firebase-session")
    public LoginResponse firebaseSession(@RequestBody FirebaseSessionRequest request) {
        return branchAuthService.firebaseSession(request);
    }
}
