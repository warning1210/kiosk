package com.kiosk.hq.branch.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branchapplication.ApprovalStatus;
import com.kiosk.domain.branchapplication.BranchApplication;
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 본점은 이메일로 가입 초대 URL만 발급한다 (BR-018/HQ-017) - 지점명/주소/지점장 정보/
// 로그인 계정은 전부 지점이 초대 URL로 join()할 때 직접 입력하므로, 여기서는 별도의
// "신청 검토/승인" 단계 없이 발급 즉시 사용 가능한 초대를 만든다.
@Service
@RequiredArgsConstructor
@Transactional
public class BranchApplicationService {

    private static final int INVITE_VALID_HOURS = 3;

    private final BranchApplicationRepository applicationRepository;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(String origin) {
        String base = origin == null ? "http://localhost:5173" : origin;
        return applicationRepository.findAllByOrderByAppliedAtDesc().stream()
                .map(a -> toResponse(a, inviteUrl(a, base)))
                .toList();
    }

    public ApplicationResponse issueInvite(String email, String origin) {
        if (applicationRepository.existsByEmailAndApprovalStatusIn(email, List.of(ApprovalStatus.APPROVED))) {
            throw new IllegalArgumentException("이미 초대를 발급한 이메일입니다. 목록에서 초대 URL을 다시 생성하세요.");
        }
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        BranchApplication application = BranchApplication.builder()
                .email(email)
                .approvalStatus(ApprovalStatus.APPROVED)
                .appliedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
        issueInviteToken(application);
        applicationRepository.save(application);

        return toResponse(application, inviteUrl(application, origin == null ? "http://localhost:5173" : origin));
    }

    // 초대 URL이 만료됐거나 재발송이 필요할 때 본점이 새로 발급한다 (기존 토큰은 즉시 무효화됨)
    public ApplicationResponse regenerateInvite(Long id, String origin) {
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        if (application.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("승인된 신청에만 초대 URL을 만들 수 있습니다.");
        }
        if (adminRepository.findByEmail(application.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 가입이 완료되어 초대 URL이 필요하지 않습니다.");
        }
        issueInviteToken(application);
        applicationRepository.save(application);
        return toResponse(application, inviteUrl(application, origin == null ? "http://localhost:5173" : origin));
    }

    private void issueInviteToken(BranchApplication application) {
        application.setInviteToken(UUID.randomUUID().toString());
        application.setInviteExpiresAt(LocalDateTime.now().plusHours(INVITE_VALID_HOURS));
    }

    public ApplicationResponse deleteAccount(Long id) {
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        if (application.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("수락 완료된 지점 계정만 삭제할 수 있습니다.");
        }
        Admin admin = adminRepository.findByEmail(application.getEmail())
                .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER)
                .orElseThrow(() -> new IllegalStateException("삭제할 지점 계정을 찾을 수 없습니다."));
        if (admin.getAccountStatus() == AccountStatus.DELETED) {
            return toResponse(application, null);
        }
        try {
            UserRecord firebaseUser = FirebaseAuth.getInstance().getUserByEmail(admin.getEmail());
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(firebaseUser.getUid()).setDisabled(true));
            FirebaseAuth.getInstance().revokeRefreshTokens(firebaseUser.getUid());
        } catch (Exception e) {
            throw new IllegalStateException("Firebase 계정을 삭제 상태로 변경하지 못했습니다: " + e.getMessage());
        }
        admin.setAccountStatus(AccountStatus.DELETED);
        adminRepository.save(admin);
        return toResponse(application, null);
    }

    // 초대 URL은 만료 전이고 아직 join()하지 않은(=계정이 없는) 승인 건에만 의미가 있다
    private String inviteUrl(BranchApplication application, String base) {
        if (application.getInviteToken() == null || application.getInviteExpiresAt() == null
                || application.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return base + "/branch/join?token=" + application.getInviteToken();
    }

    private ApplicationResponse toResponse(BranchApplication application, String inviteUrl) {
        String loginId = adminRepository.findByEmail(application.getEmail()).map(Admin::getLoginId).orElse(null);
        String accountStatus = adminRepository.findByEmail(application.getEmail())
                .map(admin -> admin.getAccountStatus().name()).orElse(null);
        return ApplicationResponse.of(application, loginId, accountStatus, inviteUrl);
    }
}
