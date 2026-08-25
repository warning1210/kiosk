package com.kiosk.global.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.BranchRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Authorization: Bearer <Firebase ID 토큰> 헤더에서 로그인한 지점 관리자의 branchId를 알아낸다.
// URL의 branchId를 그대로 신뢰하지 않고 서버가 "이 토큰의 주인이 실제로 담당하는 지점"을 검증하기 위한 용도.
@Service
@RequiredArgsConstructor
@Slf4j
public class BranchAccessService {

    private final AdminRepository adminRepository;
    private final AdminTokenService adminTokenService;
    // 인증된 지점 요청이 들어올 때마다 마지막 접속 시각을 저장한다.
    private final BranchRepository branchRepository;

    @Transactional
    public Long requireBranchId(String authorization) {
        return requireAdmin(authorization).getBranch().getBranchId();
    }

    @Transactional
    public Admin requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String token = authorization.substring(7);
        // AdminTokenService가 발급한 폴백 토큰인지 먼저 검사한다 - 형식이 다른 Firebase JWT는
        // 여기서 항상 null이 나와 안전하게 아래 Firebase 검증으로 넘어간다.
        Long fallbackAdminId = adminTokenService.verify(token);
        Admin admin = fallbackAdminId != null ? requireAdminById(fallbackAdminId) : requireAdminViaFirebase(token);
        // 단순 계정 상태가 아니라 실제 요청 시각으로 매장의 온라인 여부를 판별할 수 있게 한다.
        admin.getBranch().setKioskLastAccessAt(LocalDateTime.now());
        branchRepository.save(admin.getBranch());
        return admin;
    }

    // SSE 스트림 전용 - issueStreamTicket()이 만든 짧은 티켓만 받는다. 계정 상태 검증은
    // requireAdminById()를 그대로 재사용해 일반 로그인 경로와 동일한 규칙을 적용한다.
    @Transactional
    public Long requireBranchIdForStream(String ticket) {
        Long adminId = adminTokenService.verifyStreamTicket(ticket);
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Admin admin = requireAdminById(adminId);
        admin.getBranch().setKioskLastAccessAt(LocalDateTime.now());
        branchRepository.save(admin.getBranch());
        return admin.getBranch().getBranchId();
    }

    private Admin requireAdminById(Long adminId) {
        return adminRepository.findById(adminId)
                .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER)
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(a -> a.getBranch() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "사용할 수 없는 지점 계정입니다."));
    }

    private Admin requireAdminViaFirebase(String idToken) {
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return adminRepository.findByEmail(token.getEmail())
                    .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER)
                    .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                    .filter(a -> a.getBranch() != null)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "사용할 수 없는 지점 계정입니다."));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Firebase 로그인 토큰 검증에 실패했습니다.", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 유효하지 않습니다.");
        }
    }
}
