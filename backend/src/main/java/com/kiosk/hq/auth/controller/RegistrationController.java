package com.kiosk.hq.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiosk.hq.auth.service.RegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    // 본사에서 링크 생성 요청 시 호출
    @PostMapping("/generate-link")
    public ResponseEntity<String> generateLink() {
        String url = registrationService.createRegistrationLink();
        return ResponseEntity.ok(url);
    }

    // Vue에서 링크 접속 시 유효성 검증
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        boolean isValid = registrationService.validateToken(token);
        Map<String, Object> response = new HashMap<>();
        
        if (isValid) {
            response.put("status", "VALID");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "INVALID");
            response.put("message", "만료되었거나 유효하지 않은 링크입니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}