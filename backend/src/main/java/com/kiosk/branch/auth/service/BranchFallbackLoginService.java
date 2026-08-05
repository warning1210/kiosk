package com.kiosk.branch.auth.service;

import com.kiosk.branch.auth.dto.DbLoginRequest;
import com.kiosk.branch.auth.dto.DbLoginResponse;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.global.security.AdminTokenService;
import com.kiosk.global.security.TurnstileVerifier;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Firebase 장애 시 지점 관리자가 DB에 저장된 비밀번호 해시로 직접 로그인하는 폴백 경로.
// BranchAuthService(Firebase 로그인)와 완전히 분리된 별도 서비스 - Firebase를 전혀 호출하지 않는다.
// 토큰 발급은 본점 로그인(HqAuthService)이 이미 쓰고 있는 AdminTokenService를 그대로 재사용한다.
@Service
@RequiredArgsConstructor
@Transactional
public class BranchFallbackLoginService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenService adminTokenService;
    private final TurnstileVerifier turnstileVerifier;

    public DbLoginResponse login(DbLoginRequest request) {
       // turnstileVerifier.verify(request.turnstileToken());

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER)
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(a -> a.getBranch() != null)
                .filter(a -> matches(request.password(), a.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        String token = adminTokenService.issue(admin.getAdminId());
        return new DbLoginResponse(admin.getAdminId(), admin.getBranch().getBranchId(), admin.getBranch().getBranchName(),
                admin.getName(), token);
    }

    // 이 변경 이전에 가입한 계정은 passwordHash에 "FIREBASE$"+uid 마커만 있어 BCrypt 형식이 아니므로
    // matches()가 예외를 던진다 - 그 경우도 그냥 "불일치"로 처리해 계정 존재 여부가 새어나가지 않게 한다.
    private boolean matches(String rawPassword, String storedHash) {
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
