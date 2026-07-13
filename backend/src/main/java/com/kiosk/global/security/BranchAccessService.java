package com.kiosk.global.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Authorization: Bearer <Firebase ID 토큰> 헤더에서 로그인한 지점 관리자의 branchId를 알아낸다.
// URL의 branchId를 그대로 신뢰하지 않고 서버가 "이 토큰의 주인이 실제로 담당하는 지점"을 검증하기 위한 용도.
@Service
@RequiredArgsConstructor
public class BranchAccessService {

    private final AdminRepository adminRepository;

    public Long requireBranchId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(authorization.substring(7));
            Admin admin = adminRepository.findByEmail(token.getEmail())
                    .filter(a -> a.getRole() == AdminRole.BRANCH_MANAGER)
                    .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                    .filter(a -> a.getBranch() != null)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "사용할 수 없는 지점 계정입니다."));
            return admin.getBranch().getBranchId();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 유효하지 않습니다.");
        }
    }
}
