package com.kiosk.hq.auth.service;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.global.security.AdminTokenService;
import com.kiosk.global.security.TurnstileVerifier;
import com.kiosk.hq.auth.dto.HqLoginRequest;
import com.kiosk.hq.auth.dto.HqLoginResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HqAuthService {

    private final AdminRepository adminRepository;
    private final AdminTokenService adminTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TurnstileVerifier turnstileVerifier;

    public HqLoginResponse login(HqLoginRequest request) {
        turnstileVerifier.verify(request.turnstileToken());

        Admin admin = findByIdentifier(request.loginId())
                .filter(a -> a.getRole() == AdminRole.HQ_ADMIN || a.getRole() == AdminRole.SUPER_ADMIN)
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(a -> matches(request.password(), a.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        admin.setLastLoginAt(LocalDateTime.now());
        String token = adminTokenService.issue(admin.getAdminId());
        return new HqLoginResponse(admin.getAdminId(), admin.getName(), token);
    }

    // 입력값에 @가 있으면 이메일로, 없으면 로그인 아이디로 조회한다 (지점 로그인과 동일한 규칙)
    private Optional<Admin> findByIdentifier(String identifier) {
        return identifier.contains("@") ? adminRepository.findByEmail(identifier) : adminRepository.findByLoginId(identifier);
    }

    // 형식이 안 맞는 해시(예: 시딩 중 남은 "N/A" 같은 값)에 matches()가 예외를 던져도 그냥 "불일치"로 처리한다
    private boolean matches(String rawPassword, String storedHash) {
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
