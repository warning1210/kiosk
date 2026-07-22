package com.kiosk.branch.auth.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
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

    // 초대 과정에서 새로 만든 Firebase 사용자만 반려 시 삭제할 수 있도록 남기는 내부 표식이다.
    public static final String INVITE_PROVISIONED_CLAIM = "kioskInviteProvisioned";

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

        // 이메일이 Firebase에 이미 있으면 기존 계정을 보존하고, 없을 때만 승인 대기 계정을 새로 만든다.
        provisionFirebaseUser(application.getEmail(), request.password(), request.managerName());

        Branch branch = branchRepository.save(Branch.builder()
                .branchName(request.branchName())
                .address(request.address())
                .phone(request.phone())
                .email(application.getEmail())
                .managerName(request.managerName())
                // 본점 승인 전에는 매장을 운영 대기 상태로 저장한다.
                .operationStatus(OperationStatus.PENDING)
                .kioskStatus(KioskStatus.ACTIVE)
                .build());

        Admin admin = adminRepository.save(Admin.builder()
                .branch(branch)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.managerName())
                .phone(request.phone())
                .email(application.getEmail())
                .role(AdminRole.BRANCH_MANAGER)
                // 신청서 제출만으로 로그인할 수 없도록 승인 대기 상태로 저장한다.
                .accountStatus(AccountStatus.PENDING)
                .build());

        application.setManagerName(request.managerName());
        application.setBranchName(request.branchName());
        application.setPhone(request.phone());
        application.setAddress(request.address());
        application.setBusinessNumber(request.businessNumber());
        application.setApprovedBranch(branch);
        // 신청서 제출 뒤에도 본점이 수락하기 전까지 승인 대기 상태를 유지한다.
        application.setApprovalStatus(com.kiosk.domain.branchapplication.ApprovalStatus.PENDING);
        application.setInviteToken(null);
        // 아직 본점이 처리하지 않았으므로 처리 시각을 비워 둔다.
        application.setProcessedAt(null);
        applicationRepository.save(application);

        return new LoginResponse(admin.getAdminId(), branch.getBranchId(), branch.getBranchName(), admin.getName());
    }

    // 기존 Firebase 사용자는 로그인 정보를 바꾸지 않고 재사용하며, 신규 사용자만 임시 계정으로 생성한다.
    private void provisionFirebaseUser(String email, String password, String displayName) {
        try {
            // 같은 이메일의 사용자가 이미 있으면 UID·비밀번호·활성 상태를 그대로 유지한다.
            UserRecord existingUser = FirebaseAuth.getInstance().getUserByEmail(email);
            // 이전 가입 실패로 남은 임시 사용자라면 새 입력값으로 복구해 같은 초대를 다시 진행할 수 있게 한다.
            if (Boolean.TRUE.equals(existingUser.getCustomClaims().get(INVITE_PROVISIONED_CLAIM))) {
                FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(existingUser.getUid())
                        .setPassword(password)
                        .setDisplayName(displayName)
                        .setEmailVerified(true)
                        .setDisabled(true));
            }
            return;
        } catch (FirebaseAuthException exception) {
            // 사용자 없음 이외의 Firebase 조회 오류는 신규 생성으로 오인하지 않고 즉시 전달한다.
            if (exception.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw new IllegalArgumentException("Firebase 계정을 확인할 수 없습니다: " + exception.getMessage());
            }
        }

        UserRecord createdUser = null;
        try {
            // Firebase에 이메일이 없을 때만 본점 승인 전 비활성 상태의 사용자를 새로 만든다.
            createdUser = FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName)
                    .setEmailVerified(true)
                    .setDisabled(true));
            // 새로 만든 임시 사용자임을 기록해 반려 시 기존 사용자와 구분한다.
            FirebaseAuth.getInstance().setCustomUserClaims(createdUser.getUid(),
                    Map.of(INVITE_PROVISIONED_CLAIM, true));
        } catch (Exception exception) {
            // 권한 표식 저장이 실패하면 방금 만든 고아 사용자가 남지 않도록 정리한다.
            if (createdUser != null) {
                try {
                    FirebaseAuth.getInstance().deleteUser(createdUser.getUid());
                } catch (Exception ignored) {
                    // 원래 Firebase 오류를 보존하기 위해 정리 실패는 별도로 덮어쓰지 않는다.
                }
            }
            throw new IllegalArgumentException("Firebase 계정을 준비할 수 없습니다: " + exception.getMessage());
        }
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
