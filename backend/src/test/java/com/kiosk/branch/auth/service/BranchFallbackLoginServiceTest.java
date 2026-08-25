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
import com.kiosk.global.security.AuthCookie;
import com.kiosk.global.security.TurnstileVerifier;
import java.util.Optional;
import org.springframework.mock.web.MockHttpServletResponse;
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

    @Mock
    private TurnstileVerifier turnstileVerifier;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdminTokenService adminTokenService = new AdminTokenService("test-secret-for-hmac-signing-32-bytes");
    private final AuthCookie authCookie = new AuthCookie(false);

    private BranchFallbackLoginService service;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        service = new BranchFallbackLoginService(adminRepository, passwordEncoder, adminTokenService,
                turnstileVerifier, authCookie);
        response = new MockHttpServletResponse();
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

        DbLoginResponse body = service.login(new DbLoginRequest("gangnam1", "secret1234", "test-turnstile-token"), response);

        assertThat(body.adminId()).isEqualTo(1L);
        assertThat(body.branchId()).isEqualTo(7L);
        assertThat(body.branchName()).isEqualTo("강남점");
        // 토큰은 쿠키로만 나가고 그 쿠키는 JS가 읽을 수 없어야 한다.
        assertThat(setCookie()).contains("HttpOnly").contains("SameSite=Strict");
        assertThat(adminTokenService.verify(cookieToken())).isEqualTo(1L);
    }

    // DbLoginResponse에 token 필드가 없다는 것 자체는 컴파일 타임에 보장되지만,
    // "본문에 토큰이 새어나가지 않는다"가 이 변경의 핵심이라 값으로도 확인해 둔다.
    @Test
    void login_doesNotLeakTokenIntoResponseBody() {
        Admin admin = branchManager("secret1234");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        DbLoginResponse body = service.login(new DbLoginRequest("gangnam1", "secret1234", "test-turnstile-token"), response);

        assertThat(body.toString()).doesNotContain(cookieToken());
    }

    @Test
    void login_withWrongPassword_setsNoCookie() {
        Admin admin = branchManager("secret1234");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "nope", "test-turnstile-token"), response))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(setCookie()).isNull();
    }

    private String setCookie() {
        return response.getHeader("Set-Cookie");
    }

    /** "admin_session=<토큰>; Path=/; ..." 에서 토큰만 뽑아낸다. */
    private String cookieToken() {
        String header = setCookie();
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }

    @Test
    void login_withWrongPassword_throws() {
        Admin admin = branchManager("secret1234");
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "wrong-password", "test-turnstile-token"), response))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withUnknownLoginId_throws() {
        when(adminRepository.findByLoginId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new DbLoginRequest("nobody", "secret1234", "test-turnstile-token"), response))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withNonBranchManagerRole_throws() {
        Admin admin = branchManager("secret1234");
        admin.setRole(AdminRole.HQ_ADMIN);
        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "secret1234", "test-turnstile-token"), response))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_withLegacyFirebaseMarkerHash_throws() {
        Admin admin = branchManager("secret1234");
        admin.setPasswordHash("FIREBASE$some-uid");

        when(adminRepository.findByLoginId("gangnam1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login(new DbLoginRequest("gangnam1", "secret1234", "test-turnstile-token"), response))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
