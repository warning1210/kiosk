package com.kiosk.branch.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kiosk.branch.auth.dto.DbLoginRequest;
import com.kiosk.branch.auth.dto.DbLoginResponse;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.global.security.AdminTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BranchFallbackLoginServiceTest {

    @Mock
    private AdminRepository adminRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdminTokenService adminTokenService = new AdminTokenService("test-secret");

    private BranchFallbackLoginService service;

    @BeforeEach
    void setUp() {
        service = new BranchFallbackLoginService(adminRepository, passwordEncoder, adminTokenService);
    }

    private Admin branchManager(String rawPassword) {
        Branch branch = Branch.builder().branchId(7L).branchName("강남점").build();
        return Admin.builder()
                .adminId(1L)
                .branch(branch)
                .loginId("gangnam1")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name("홍길동")
                .role(AdminRole.BRANCH_MANAGER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void login_withCorrectPassword_issuesTokenResolvingToSameAdmin() {
        Admin admin = branchManager("secret1234");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        DbLoginResponse response = service.login(new DbLoginRequest("gangnam1", "secret1234"));

        assertThat(response.adminId()).isEqualTo(1L);
        assertThat(response.branchId()).isEqualTo(7L);
        assertThat(response.branchName()).isEqualTo("강남점");
        assertThat(adminTokenService.verify(response.token())).isEqualTo(1L);
    }

    @Test
    void login_withWrongPassword_throws() {
        Admin admin = branchManager("secret1234");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withUnknownLoginId_throws() {
        when(adminRepository.findByLoginId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new DbLoginRequest("nobody", "secret1234")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withNonBranchManagerRole_throws() {
        Admin admin = branchManager("secret1234");
        admin.setRole(AdminRole.HQ_ADMIN);
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "secret1234")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withLegacyFirebaseMarkerHash_throws() {
        Admin admin = branchManager("secret1234");
        admin.setPasswordHash("FIREBASE$some-uid");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "secret1234")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
