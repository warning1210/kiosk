package com.kiosk.global.config;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 본점 고정 관리자 계정을 앱 기동 시 없으면 만든다 (공유 DB에 수동 SQL 없이 팀원 모두 동일 계정 사용).
@Component
@RequiredArgsConstructor
public class HqAdminSeeder implements CommandLineRunner {

    private static final String DEFAULT_LOGIN_ID = "hadmin";
    private static final String DEFAULT_PASSWORD = "hadmin1234";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminRepository.findByLoginId(DEFAULT_LOGIN_ID).isPresent()) return;

        adminRepository.save(Admin.builder()
                .loginId(DEFAULT_LOGIN_ID)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .name("본점 관리자")
                .role(AdminRole.HQ_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build());
    }
}
