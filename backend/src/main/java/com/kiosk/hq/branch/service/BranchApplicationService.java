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
import com.kiosk.domain.branch.OperationStatus;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
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
    // 반려 시 승인 대기로 만들어 둔 임시 지점 데이터를 삭제한다.
    private final BranchRepository branchRepository;
    // 초대 링크와 승인 결과를 이메일로 발송한다.
    private final BranchInviteMailService mailService;
    // 이메일과 복사 버튼에 사용할 외부 접속 가능한 프론트 주소다.
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(String origin) {
        // 요청 브라우저의 localhost가 아니라 서버에 설정된 공개 주소를 사용한다.
        String base = frontendBaseUrl;
        return applicationRepository.findAllByOrderByAppliedAtDesc().stream()
                .map(a -> toResponse(a, inviteUrl(a, base)))
                .toList();
    }

    public ApplicationResponse issueInvite(String email, String origin) {
        // 같은 이메일로 이전에 발급한 초대 또는 제출한 신청이 있는지 확인한다.
        BranchApplication existing = applicationRepository.findFirstByEmailOrderByCreatedAtDesc(email).orElse(null);
        // 아직 신청서를 제출하지 않은 초대라면 새 토큰으로 갱신해 다시 발송한다.
        if (existing != null && existing.getManagerName() == null && existing.getApprovalStatus() == ApprovalStatus.PENDING) {
            // 이전 토큰을 폐기하고 새 일회용 토큰을 만든다.
            issueInviteToken(existing);
            // 갱신된 초대 정보를 저장한다.
            applicationRepository.save(existing);
            // 설정된 외부 주소로 새 신청서 URL을 만든다.
            String retryUrl = inviteUrl(existing, frontendBaseUrl);
            // 동일 이메일로 신청서 URL을 다시 발송한다.
            mailService.sendInvite(email, retryUrl);
            // 본점 화면에서도 새 URL을 확인할 수 있도록 반환한다.
            return toResponse(existing, retryUrl);
        }
        // 신청서를 이미 제출했거나 승인된 이메일은 중복 신청할 수 없다.
        if (existing != null && existing.getApprovalStatus() != ApprovalStatus.REJECTED) throw new IllegalArgumentException("이미 제출 또는 승인 처리 중인 이메일입니다.");
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        BranchApplication application = BranchApplication.builder()
                .email(email)
                // 이메일 발급만 한 상태는 아직 본점 승인 전이므로 대기로 저장한다.
                .approvalStatus(ApprovalStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();
        issueInviteToken(application);
        applicationRepository.save(application);

        // 지점장이 작성할 신청서 URL을 만든다.
        // 다른 PC의 예비 지점장도 열 수 있는 설정 주소로 신청서 URL을 만든다.
        String url = inviteUrl(application, frontendBaseUrl);
        // 설정된 SMTP 서버로 신청서 URL을 발송한다.
        mailService.sendInvite(email, url);
        // 화면에서도 복사할 수 있도록 발급 결과와 URL을 반환한다.
        return toResponse(application, url);
    }

    // 초대 URL이 만료됐거나 재발송이 필요할 때 본점이 새로 발급한다 (기존 토큰은 즉시 무효화됨)
    public ApplicationResponse regenerateInvite(Long id, String origin) {
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        // 이미 신청서를 제출한 건에는 새 초대 URL을 만들 수 없다.
        if (application.getManagerName() != null) throw new IllegalStateException("이미 제출된 신청서입니다.");
        if (adminRepository.findByEmail(application.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 가입이 완료되어 초대 URL이 필요하지 않습니다.");
        }
        issueInviteToken(application);
        applicationRepository.save(application);
        // 새로 만든 URL을 계산한다.
        // 재발급 URL도 같은 외부 접속 주소를 사용한다.
        String url = inviteUrl(application, frontendBaseUrl);
        // 재발급된 URL도 같은 이메일로 다시 발송한다.
        mailService.sendInvite(application.getEmail(), url);
        // 화면에 최신 URL을 반환한다.
        return toResponse(application, url);
    }

    // 본점이 제출된 신청서를 수락해 지점과 로그인을 활성화한다.
    public ApplicationResponse approve(Long id, Admin processor) {
        // 처리할 신청서를 식별자로 조회한다.
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        // 신청서가 제출되지 않은 초대는 승인할 수 없다.
        if (application.getApprovedBranch() == null || application.getManagerName() == null) throw new IllegalStateException("아직 제출되지 않은 신청서입니다.");
        // 신청 이메일과 연결된 지점장 계정을 찾는다.
        Admin admin = adminRepository.findByEmail(application.getEmail()).orElseThrow(() -> new IllegalStateException("지점장 계정을 찾을 수 없습니다."));
        // Firebase 계정을 활성화해 로그인을 허용한다.
        try {
            // Firebase에서 신청 이메일의 사용자를 조회한다.
            UserRecord firebaseUser = FirebaseAuth.getInstance().getUserByEmail(application.getEmail());
            // 비활성 상태를 해제한다.
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(firebaseUser.getUid()).setDisabled(false));
        } catch (Exception e) {
            // Firebase 활성화 실패 시 DB 승인도 진행하지 않는다.
            throw new IllegalStateException("Firebase 계정을 활성화하지 못했습니다: " + e.getMessage());
        }
        // 지점장 계정을 정상 상태로 변경한다.
        admin.setAccountStatus(AccountStatus.ACTIVE);
        // 지점 운영 상태를 정상으로 변경한다.
        application.getApprovedBranch().setOperationStatus(OperationStatus.ACTIVE);
        // 신청 상태를 승인 완료로 변경한다.
        application.setApprovalStatus(ApprovalStatus.APPROVED);
        // 처리한 본점 관리자를 기록한다.
        application.setProcessedAdmin(processor);
        // 처리 시각을 현재 시각으로 기록한다.
        application.setProcessedAt(LocalDateTime.now());
        // 계정 변경을 저장한다.
        adminRepository.save(admin);
        // 신청 변경을 저장한다.
        applicationRepository.save(application);
        // 지점장에게 승인 완료 결과를 발송한다.
        mailService.sendResult(application.getEmail(), true, null);
        // 본점 화면에 최신 신청 정보를 반환한다.
        return toResponse(application, null);
    }

    // 본점이 제출된 신청서를 반려하고 로그인을 계속 차단한다.
    public ApplicationResponse reject(Long id, String reason, Admin processor) {
        // 반려할 신청서를 조회한다.
        BranchApplication application = applicationRepository.findById(id).orElseThrow();
        // 예비 지점장이 이유를 알 수 있도록 빈 반려 사유를 허용하지 않는다.
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
        // 삭제할 임시 지점장 계정을 이메일로 찾는다.
        Admin pendingAdmin = adminRepository.findByEmail(application.getEmail()).orElse(null);
        // 삭제할 임시 지점을 신청 데이터에서 가져온다.
        Branch pendingBranch = application.getApprovedBranch();
        // 신청을 반려 상태로 변경한다.
        application.setApprovalStatus(ApprovalStatus.REJECTED);
        // 본점 관리자가 입력한 반려 사유를 저장한다.
        application.setRejectionReason(reason.trim());
        // 처리한 본점 관리자를 기록한다.
        application.setProcessedAdmin(processor);
        // 반려 처리 시각을 기록한다.
        application.setProcessedAt(LocalDateTime.now());
        // 삭제 전에 본점 화면과 결과 처리에 사용할 반려 응답을 만든다.
        ApplicationResponse rejectedResponse = toResponse(application, null);
        // 지점장에게 반려 결과 메일을 발송한다.
        mailService.sendResult(application.getEmail(), false, application.getRejectionReason());
        // Firebase에 만들어 둔 승인 대기 사용자를 삭제한다.
        try {
            // 신청 이메일에 해당하는 Firebase 사용자를 조회한다.
            UserRecord firebaseUser = FirebaseAuth.getInstance().getUserByEmail(application.getEmail());
            // 반려된 사용자가 로그인 계정으로 남지 않도록 완전히 삭제한다.
            FirebaseAuth.getInstance().deleteUser(firebaseUser.getUid());
        } catch (Exception e) {
            // Firebase 삭제 실패 시 DB만 삭제되어 불일치하지 않도록 처리를 중단한다.
            throw new IllegalStateException("Firebase 임시 계정을 삭제하지 못했습니다: " + e.getMessage());
        }
        // 지점 외래키를 참조하는 신청 데이터를 먼저 삭제한다.
        applicationRepository.delete(application);
        // 승인 대기 지점장 계정이 있으면 DB에서 삭제한다.
        if (pendingAdmin != null) adminRepository.delete(pendingAdmin);
        // 승인 대기 지점이 있으면 DB에서 삭제한다.
        if (pendingBranch != null) branchRepository.delete(pendingBranch);
        // 삭제 전 만들어 둔 반려 결과를 본점 화면에 반환한다.
        return rejectedResponse;
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
        // 외부 접속 주소 뒤에 예비 지점장 전용 신청서 경로와 일회용 토큰을 붙인다.
        return base + "/branch/application?token=" + application.getInviteToken();
    }

    private ApplicationResponse toResponse(BranchApplication application, String inviteUrl) {
        String loginId = adminRepository.findByEmail(application.getEmail()).map(Admin::getLoginId).orElse(null);
        String accountStatus = adminRepository.findByEmail(application.getEmail())
                .map(admin -> admin.getAccountStatus().name()).orElse(null);
        return ApplicationResponse.of(application, loginId, accountStatus, inviteUrl);
    }
}
