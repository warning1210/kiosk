package com.kiosk.hq.branch.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branch.KioskStatus;
import com.kiosk.domain.branch.OperationStatus;
import com.kiosk.domain.branchapplication.ApprovalStatus;
import com.kiosk.domain.branchapplication.BranchApplication;
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 본점이 지점 개설 신청을 조회/승인/거절하고, 필요 시 지점 계정을 삭제(비활성화)한다.
@Service
@RequiredArgsConstructor
@Transactional
public class BranchApplicationService {

    private final BranchApplicationRepository applicationRepository;
    private final BranchRepository branchRepository;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(String origin) {
        String base = origin == null ? "http://localhost:5173" : origin;
        return applicationRepository.findAllByOrderByAppliedAtDesc().stream()
                .map(a -> toResponse(a, a.getInviteToken() == null ? null : base + "/branch/join?token=" + a.getInviteToken()))
                .toList();
    }

    public ApplicationResponse approve(Long id) {
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        if (application.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        Admin admin = adminRepository.findByEmail(application.getEmail())
                .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER && a.getAccountStatus() == AccountStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("이 신청에는 로그인 계정 정보가 없습니다. 지점에서 다시 신청해야 합니다."));

        Branch branch = branchRepository.save(Branch.builder()
                .branchName(application.getBranchName())
                .address(application.getAddress())
                .phone(application.getPhone())
                .email(application.getEmail())
                .managerName(application.getManagerName())
                .operationStatus(OperationStatus.PENDING)
                .kioskStatus(KioskStatus.ACTIVE)
                .build());

        application.setApprovalStatus(ApprovalStatus.APPROVED);
        application.setApprovedBranch(branch);
        application.setInviteToken(null);
        application.setInviteExpiresAt(null);
        application.setProcessedAt(LocalDateTime.now());
        applicationRepository.save(application);

        admin.setBranch(branch);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        adminRepository.save(admin);

        try {
            String uid = admin.getPasswordHash().substring("FIREBASE$".length());
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(uid).setDisabled(false));
            FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", "BRANCH_MANAGER", "branchId", branch.getBranchId()));
        } catch (Exception e) {
            throw new IllegalStateException("Firebase 계정을 활성화하지 못했습니다: " + e.getMessage());
        }

        branch.setOperationStatus(OperationStatus.ACTIVE);
        branchRepository.save(branch);

        return toResponse(application, null);
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

    private ApplicationResponse toResponse(BranchApplication application, String inviteUrl) {
        String loginId = adminRepository.findByEmail(application.getEmail()).map(Admin::getLoginId).orElse(null);
        String accountStatus = adminRepository.findByEmail(application.getEmail())
                .map(admin -> admin.getAccountStatus().name()).orElse(null);
        return ApplicationResponse.of(application, loginId, accountStatus, inviteUrl);
    }
}
