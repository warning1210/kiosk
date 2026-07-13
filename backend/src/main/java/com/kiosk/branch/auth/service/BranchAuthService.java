package com.kiosk.branch.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.branch.auth.dto.ApplyRequest;
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
import com.kiosk.domain.branch.OperationStatus;
import com.kiosk.domain.branchapplication.ApprovalStatus;
import com.kiosk.domain.branchapplication.BranchApplication;
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지점 회원가입/로그인 - 실제 비밀번호 검증은 Firebase Auth에 위임하고,
// Admin.passwordHash에는 "FIREBASE$"+uid만 표시용으로 저장한다(우리 쪽에서 별도 해시 비교 안 함).
@Service
@RequiredArgsConstructor
@Transactional
public class BranchAuthService {

    private final BranchApplicationRepository applicationRepository;
    private final BranchRepository branchRepository;
    private final AdminRepository adminRepository;

    public ApplicationResponse apply(ApplyRequest request) {
        if (request.loginId() == null || request.loginId().trim().length() < 4) {
            throw new IllegalArgumentException("아이디는 4자 이상 입력하세요.");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
        }
        if (adminRepository.existsByLoginId(request.loginId().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (adminRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("이미 신청되었거나 사용 중인 이메일입니다.");
        }

        UserRecord firebaseUser;
        try {
            firebaseUser = FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(request.email())
                    .setPassword(request.password())
                    .setDisplayName(request.managerName())
                    .setEmailVerified(true)
                    .setDisabled(true));
        } catch (Exception e) {
            throw new IllegalArgumentException("계정 신청 정보를 등록할 수 없습니다: " + e.getMessage());
        }

        adminRepository.save(Admin.builder()
                .loginId(request.loginId().trim())
                .passwordHash("FIREBASE$" + firebaseUser.getUid())
                .name(request.managerName())
                .phone(request.phone())
                .email(request.email())
                .role(AdminRole.BRANCH_MANAGER)
                .accountStatus(AccountStatus.PENDING)
                .build());

        BranchApplication saved = applicationRepository.save(BranchApplication.builder()
                .branchName(request.branchName())
                .managerName(request.managerName())
                .phone(request.phone())
                .email(request.email())
                .address(request.address())
                .businessNumber(request.businessNumber())
                .approvalStatus(ApprovalStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build());

        return toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse invite(String token) {
        return toResponse(validInvite(token), null);
    }

    public LoginResponse join(JoinRequest request) {
        BranchApplication application = validInvite(request.token());
        if (adminRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        Branch branch = application.getApprovedBranch();

        UserRecord firebaseUser;
        try {
            firebaseUser = FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(application.getEmail())
                    .setPassword(request.password())
                    .setDisplayName(application.getManagerName())
                    .setEmailVerified(true));
            FirebaseAuth.getInstance().setCustomUserClaims(firebaseUser.getUid(),
                    Map.of("role", "BRANCH_MANAGER", "branchId", branch.getBranchId()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Firebase 계정을 만들 수 없습니다: " + e.getMessage());
        }

        Admin admin = adminRepository.save(Admin.builder()
                .branch(branch)
                .loginId(request.loginId())
                .passwordHash("FIREBASE$" + firebaseUser.getUid())
                .name(application.getManagerName())
                .phone(application.getPhone())
                .email(application.getEmail())
                .role(AdminRole.BRANCH_MANAGER)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        branch.setOperationStatus(OperationStatus.ACTIVE);
        branchRepository.save(branch);
        application.setInviteToken(null);
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
            throw new IllegalArgumentException("Firebase 로그인 토큰이 유효하지 않습니다.");
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
