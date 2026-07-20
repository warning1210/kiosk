package com.kiosk.hq.auth.service;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.global.security.AdminTokenService;
import com.kiosk.hq.auth.dto.HqLoginRequest;
import com.kiosk.hq.auth.dto.HqLoginResponse;
import java.time.LocalDateTime;
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

    public HqLoginResponse login(HqLoginRequest request) {
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .filter(a -> a.getRole() == AdminRole.HQ_ADMIN || a.getRole() == AdminRole.SUPER_ADMIN)
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        String token = adminTokenService.issue(admin.getAdminId());
        return new HqLoginResponse(admin.getAdminId(), admin.getName(), token);
    }
}
