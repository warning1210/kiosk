package com.kiosk.branch.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.branch.auth.dto.FirebaseSessionRequest;
import com.kiosk.branch.auth.dto.JoinRequest;
import com.kiosk.branch.auth.dto.LoginIdentityResponse;
import com.kiosk.branch.auth.dto.LoginResponse;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branch.KioskStatus;
import com.kiosk.domain.branch.OperationStatus;
import com.kiosk.domain.branchapplication.BranchApplication;
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지점 회원가입/로그인(이메일) - 평상시 비밀번호 검증은 Firebase Auth에 위임한다.
// Admin.passwordHash에는 Firebase 계정 생성과 동시에 BCrypt 해시도 저장해두는데, 이건 Firebase 로그인
// 경로에서는 쓰이지 않고 Firebase 장애 시의 폴백 로그인(BranchFallbackLoginService, 완전히 별도 파일)이
// 쓴다 - 이 클래스는 그 폴백 경로와 무관하게 기존 Firebase 흐름만 그대로 담당한다.
// 본점은 이메일로 가입 초대 URL만 발급하고(BranchApplicationService.issueInvite),
// 지점명/주소/지점장 정보/로그인 계정은 전부 지점이 join()할 때 직접 입력한다 (BR-018/HQ-017).
@Service
@RequiredArgsConstructor
@Transactional
public class BranchAuthService {

    private final BranchApplicationRepository applicationRepository;
    private final BranchRepository branchRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ApplicationResponse invite(String token) {
        return toResponse(validInvite(token), null);
    }

    public LoginResponse join(JoinRequest request) {
        BranchApplication application = validInvite(request.token());
        if (adminRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        UserRecord firebaseUser;
        try {
            firebaseUser = FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(application.getEmail())
                    .setPassword(request.password())
                    .setDisplayName(request.managerName())
                    .setEmailVerified(true));
        } catch (Exception e) {
            throw new IllegalArgumentException("Firebase 계정을 만들 수 없습니다: " + e.getMessage());
        }

        Branch branch = branchRepository.save(Branch.builder()
                .branchName(request.branchName())
                .address(request.address())
                .phone(request.phone())
                .email(application.getEmail())
                .managerName(request.managerName())
                .operationStatus(OperationStatus.ACTIVE)
                .kioskStatus(KioskStatus.ACTIVE)
                .build());

        try {
            FirebaseAuth.getInstance().setCustomUserClaims(firebaseUser.getUid(),
                    Map.of("role", "BRANCH_MANAGER", "branchId", branch.getBranchId()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Firebase 권한 설정에 실패했습니다: " + e.getMessage());
        }

        Admin admin = adminRepository.save(Admin.builder()
                .branch(branch)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.managerName())
                .phone(request.phone())
                .email(application.getEmail())
                .role(AdminRole.BRANCH_MANAGER)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        application.setManagerName(request.managerName());
        application.setBranchName(request.branchName());
        application.setPhone(request.phone());
        application.setAddress(request.address());
        application.setBusinessNumber(request.businessNumber());
        application.setApprovedBranch(branch);
        application.setInviteToken(null);
        application.setProcessedAt(LocalDateTime.now());
        applicationRepository.save(application);

        return new LoginResponse(admin.getAdminId(), branch.getBranchId(), branch.getBranchName(), admin.getName());
    }

    @Transactional(readOnly = true)
    public LoginIdentityResponse loginIdentity(String loginId) {
        Admin admin = adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 아이디입니다."));
        if (admin.getRole() != AdminRole.BRANCH_MANAGER || admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }
        return new LoginIdentityResponse(admin.getEmail());
    }

    public LoginResponse firebaseSession(FirebaseSessionRequest request) {
        FirebaseToken token;
        try {
            token = FirebaseAuth.getInstance().verifyIdToken(request.idToken(), true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Firebase 로그인 토큰이 유효하지 않습니다. 원인: " + e.getMessage());
        }
        Admin admin = adminRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("승인된 지점 계정이 아닙니다."));
        if (admin.getRole() != AdminRole.BRANCH_MANAGER || admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        return new LoginResponse(admin.getAdminId(), admin.getBranch().getBranchId(), admin.getBranch().getBranchName(), admin.getName());
    }

    private BranchApplication validInvite(String token) {
        BranchApplication application = applicationRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 URL입니다."));
        if (application.getInviteExpiresAt() == null || application.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("초대 URL이 만료되었습니다.");
        }
        return application;
    }

    private ApplicationResponse toResponse(BranchApplication application, String inviteUrl) {
        String loginId = adminRepository.findByEmail(application.getEmail()).map(Admin::getLoginId).orElse(null);
        String accountStatus = adminRepository.findByEmail(application.getEmail())
                .map(admin -> admin.getAccountStatus().name()).orElse(null);
        return ApplicationResponse.of(application, loginId, accountStatus, inviteUrl);
    }
}
