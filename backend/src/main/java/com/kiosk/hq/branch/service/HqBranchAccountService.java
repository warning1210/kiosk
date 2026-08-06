package com.kiosk.hq.branch.service;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branchapplication.ApprovalStatus;
import com.kiosk.domain.branchapplication.BranchApplication;
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import com.kiosk.global.security.CrlfUtils;
import com.kiosk.hq.branch.dto.HqBranchAccountResponse;
import com.kiosk.hq.branch.mapper.HqBranchAccountMapper;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseApp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 본점에서 실제로 생성된 모든 지점장 계정을 조회하고 관리한다.
@Service
@Slf4j
// 필요한 저장소를 생성자로 자동 주입한다.
@RequiredArgsConstructor
// 계정 상태 변경을 하나의 DB 작업으로 처리한다.
@Transactional
public class HqBranchAccountService {

    // 지점이 15초 안에 신호를 보냈으면 현재 온라인으로 표시한다.
    private static final int ONLINE_WINDOW_SECONDS = 15;

    // 지점장 계정을 조회하고 저장하는 저장소다.
    private final AdminRepository adminRepository;
    // 계정이 아직 연결되지 않은 기존 지점까지 목록에 포함하기 위해 사용한다.
    private final BranchRepository branchRepository;
    // 실제 가입 승인 대기 신청에 연결된 임시 지점만 목록에서 제외하기 위해 사용한다.
    private final BranchApplicationRepository applicationRepository;
    // 폐업 시 외래키 순서에 맞춰 해당 지점의 업무 데이터를 완전히 제거한다.
    private final HqBranchAccountMapper hqBranchAccountMapper;

    // 현재 DB에 만들어진 모든 지점장 계정을 반환한다.
    @Transactional(readOnly = true)
    public List<HqBranchAccountResponse> list() {
        // 신청 목록을 한 번만 조회해 대기 지점과 삭제 계정 지점을 함께 판별한다.
        List<BranchApplication> applications = applicationRepository.findAllByOrderByAppliedAtDesc();
        // 정상 또는 정지 상태인 지점장 계정을 지점 식별자로 묶는다.
        Map<Long, Admin> accountByBranch = adminRepository.findByRoleOrderByAdminIdAsc(AdminRole.BRANCH_MANAGER).stream()
                // 승인 전 계정과 반려·삭제 계정은 정상 계정 연결에서 제외한다.
                .filter(admin -> admin.getBranch() != null)
                // 정상 또는 정지된 운영 계정만 연결 정보로 사용한다.
                .filter(admin -> admin.getAccountStatus() == AccountStatus.ACTIVE || admin.getAccountStatus() == AccountStatus.SUSPENDED)
                // 지점 하나당 지점장 계정 하나를 찾을 수 있는 Map을 만든다.
                .collect(Collectors.toMap(admin -> admin.getBranch().getBranchId(), Function.identity(), (first, second) -> first));
        // 신청서를 제출했지만 본점이 아직 수락하지 않은 임시 지점 식별자를 모은다.
        Set<Long> pendingApplicationBranchIds = applications.stream()
                // 승인 대기 상태인 신청만 선택한다.
                .filter(application -> application.getApprovalStatus() == ApprovalStatus.PENDING)
                // 아직 신청서를 제출하지 않은 이메일 초대는 연결 지점이 없으므로 제외한다.
                .filter(application -> application.getApprovedBranch() != null)
                // 연결된 임시 지점 식별자만 추출한다.
                .map(application -> application.getApprovedBranch().getBranchId())
                // 빠른 포함 여부 확인을 위해 Set으로 만든다.
                .collect(Collectors.toSet());
        // 발급 내역에서 계정을 삭제한 승인 지점은 위쪽 계정 목록에서도 제외한다.
        Set<Long> deletedAccountBranchIds = applications.stream()
                // 승인되어 실제 지점과 연결된 신청만 검사한다.
                .filter(application -> application.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(application -> application.getApprovedBranch() != null)
                // 같은 이메일의 계정이 삭제 상태이거나 이미 제거되어 없으면 삭제 완료로 판단한다.
                .filter(application -> adminRepository.findByEmail(application.getEmail())
                        .map(admin -> admin.getAccountStatus() == AccountStatus.DELETED)
                        .orElse(true))
                // 삭제된 계정에 연결된 지점 ID를 목록 제외 대상으로 만든다.
                .map(application -> application.getApprovedBranch().getBranchId())
                .collect(Collectors.toSet());
        // DB에 이미 존재하는 운영 지점을 기준으로 전체 목록을 만든다.
        return branchRepository.findAll().stream()
                // 상태값과 관계없이 기존 DB 지점은 표시하고, 실제 승인 대기 신청 지점만 제외한다.
                .filter(branch -> !pendingApplicationBranchIds.contains(branch.getBranchId()))
                // 계정 삭제가 완료된 지점은 계정 관리 목록에 다시 미연결로 나타나지 않게 한다.
                .filter(branch -> !deletedAccountBranchIds.contains(branch.getBranchId()))
                // 지점 식별자 순서로 정렬해 목록 순서를 안정적으로 유지한다.
                .sorted(java.util.Comparator.comparing(Branch::getBranchId))
                // 계정이 있으면 실제 계정 정보를, 없으면 미연결 정보를 만든다.
                .map(branch -> toResponse(branch, accountByBranch.get(branch.getBranchId())))
                // 변환 결과를 수정할 수 없는 목록으로 만든다.
                .toList();
    }

    // 본점이 지점장 계정을 정상 또는 정지 상태로 변경한다.
    public HqBranchAccountResponse changeStatus(Long adminId, String status) {
        // 계정 식별자로 실제 계정을 찾는다.
        Admin admin = adminRepository.findById(adminId)
                // 계정이 없으면 이해하기 쉬운 오류를 발생시킨다.
                .orElseThrow(() -> new IllegalArgumentException("지점장 계정을 찾을 수 없습니다."));
        // 본점 계정이 잘못 변경되지 않도록 지점장 역할만 허용한다.
        if (admin.getRole() != AdminRole.BRANCH_MANAGER) throw new IllegalArgumentException("지점장 계정만 변경할 수 있습니다.");
        // 계정 관리 화면에서는 정상과 정지 상태만 선택할 수 있게 제한한다.
        AccountStatus nextStatus = AccountStatus.valueOf(status);
        // 삭제 상태 등 허용하지 않은 상태 변경을 차단한다.
        if (nextStatus != AccountStatus.ACTIVE && nextStatus != AccountStatus.SUSPENDED) throw new IllegalArgumentException("정상 또는 정지 상태만 선택할 수 있습니다.");
        // 선택한 상태를 지점장 계정에 반영한다.
        admin.setAccountStatus(nextStatus);
        // 변경된 계정을 DB에 저장한다.
        return toResponse(adminRepository.save(admin));
    }

    // Admin 엔티티를 본점 계정 관리 응답으로 변환한다.
    private HqBranchAccountResponse toResponse(Admin admin) {
        // 지점장 계정은 반드시 지점과 연결되어 있어야 한다.
        if (admin.getBranch() == null) throw new IllegalStateException("지점이 연결되지 않은 지점장 계정입니다.");
        // 화면에 필요한 값만 골라 응답 객체를 만든다.
        return toResponse(admin.getBranch(), admin);
    }

    // 기존 지점과 선택적으로 연결된 지점장 계정을 응답으로 변환한다.
    private HqBranchAccountResponse toResponse(Branch branch, Admin admin) {
        // 계정이 없어도 지점 정보가 목록에 표시되도록 null 값을 허용한다.
        return new HqBranchAccountResponse(
                // 계정 식별자를 전달한다.
                admin == null ? null : admin.getAdminId(),
                // 로그인 아이디를 전달한다.
                admin == null ? null : admin.getLoginId(),
                // 연결 지점 식별자를 전달한다.
                branch.getBranchId(),
                // 연결 지점명을 전달한다.
                branch.getBranchName(),
                // 지점장 이름을 전달한다.
                admin == null ? branch.getManagerName() : admin.getName(),
                // 지점장 연락처를 전달한다.
                admin == null ? branch.getPhone() : admin.getPhone(),
                // 초대받은 이메일을 전달한다.
                admin == null ? branch.getEmail() : admin.getEmail(),
                // 마지막 로그인 시각을 전달한다.
                admin == null ? null : admin.getLastLoginAt(),
                // 현재 계정 상태를 문자열로 전달한다.
                admin == null ? "UNLINKED" : admin.getAccountStatus().name(),
                // 최초 계정 생성 시각을 전달한다.
                admin == null ? branch.getCreatedAt() : admin.getCreatedAt(),
                // 계정의 권한과 수정 시각은 계정이 연결된 경우에만 전달한다.
                admin == null ? null : admin.getRole().name(),
                admin == null ? null : admin.getUpdatedAt(),
                // 지점 테이블에 저장된 나머지 상세 정보를 그대로 전달한다.
                branch.getRegion(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getOperationStatus().name(),
                branch.getOpeningDate(),
                branch.getIsBusy(),
                branch.getEstimatedWaitMinutes(),
                branch.getKioskCode(),
                branch.getKioskStatus().name(),
                branch.getKioskLastAccessAt(),
                branch.getCreatedAt(),
                branch.getUpdatedAt(),
                branch.getKioskLastAccessAt() != null
                        && branch.getKioskLastAccessAt().isAfter(LocalDateTime.now().minusSeconds(ONLINE_WINDOW_SECONDS)));
    }

    // 폐업은 Firebase 로그인과 해당 지점의 DB 업무 데이터를 함께 영구 삭제한다.
    public void closeBranch(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .filter(account -> account.getRole() == AdminRole.BRANCH_MANAGER)
                .filter(account -> account.getBranch() != null)
                .orElseThrow(() -> new IllegalArgumentException("폐업할 지점 계정을 찾을 수 없습니다."));
        Long branchId = admin.getBranch().getBranchId();

        // Firebase 삭제에 실패하면 DB 삭제를 시작하지 않아 두 저장소가 엇갈리는 상황을 줄인다.
        deleteFirebaseUser(admin.getEmail());

        // 주문·재고·채팅처럼 지점을 직접 또는 간접 참조하는 자식 행부터 지운다.
        hqBranchAccountMapper.deleteBranchChatMessages(branchId);
        hqBranchAccountMapper.deleteAdminChatMessages(adminId);
        hqBranchAccountMapper.deleteChatRooms(branchId);
        hqBranchAccountMapper.deleteInventoryTransactions(branchId);
        hqBranchAccountMapper.deleteStockRequestItems(branchId);
        hqBranchAccountMapper.deleteStockRequests(branchId);
        hqBranchAccountMapper.clearCouponOrders(branchId);
        hqBranchAccountMapper.deletePayments(branchId);
        hqBranchAccountMapper.deleteOrderItemFlavors(branchId);
        hqBranchAccountMapper.deleteOrderItems(branchId);
        hqBranchAccountMapper.deleteOrders(branchId);
        hqBranchAccountMapper.deleteBranchInventory(branchId);
        hqBranchAccountMapper.deleteBranchProducts(branchId);
        hqBranchAccountMapper.deleteEventBranchFlavors(branchId);
        hqBranchAccountMapper.deleteAuditLogs(adminId);
        // 다른 업무 행의 선택적 처리자 참조를 비워 관리자 외래키 삭제를 안전하게 만든다.
        hqBranchAccountMapper.clearIssuedByAdmin(adminId);
        hqBranchAccountMapper.clearApplicationProcessedAdmin(adminId);
        hqBranchAccountMapper.clearStockRequestProcessedAdmin(adminId);
        hqBranchAccountMapper.clearReceiptConfirmedAdmin(adminId);
        hqBranchAccountMapper.clearInventoryProcessedAdmin(adminId);
        hqBranchAccountMapper.clearAssignedAdmin(adminId);
        hqBranchAccountMapper.clearInviterAdmin(adminId);
        hqBranchAccountMapper.deleteBranchApplications(branchId);
        hqBranchAccountMapper.deleteAdmin(adminId);
        hqBranchAccountMapper.deleteBranch(branchId);
    }

    // 이미 Firebase에서 사라진 사용자는 폐업 완료로 간주하고 그 밖의 오류만 사용자에게 전달한다.
    private void deleteFirebaseUser(String email) {
        if (FirebaseApp.getApps().isEmpty()) {
        	log.warn(
        		    "Firebase 사용자 삭제 실패: {}",
        		    CrlfUtils.forLog(email)
        		);
            return;
        }
        try {
            String uid = FirebaseAuth.getInstance().getUserByEmail(email).getUid();
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw new IllegalStateException("Firebase 계정을 삭제하지 못했습니다: " + e.getMessage());
            }
        }
    }

}
