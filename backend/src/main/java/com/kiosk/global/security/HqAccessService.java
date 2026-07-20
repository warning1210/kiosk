package com.kiosk.global.security;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Authorization: Bearer <AdminTokenService 발급 토큰> 헤더에서 로그인한 본점 관리자를 알아낸다.
// BranchAccessService와 동일한 역할이지만, 본점은 Firebase 대신 자체 로그인(hadmin 등)을 쓴다.
@Service
@RequiredArgsConstructor
public class HqAccessService {

    private final AdminTokenService adminTokenService;
    private final AdminRepository adminRepository;

    public Admin requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long adminId = adminTokenService.verify(authorization.substring(7));
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 유효하지 않습니다.");
        }
        return adminRepository.findById(adminId)
                .filter(a -> a.getRole() == AdminRole.HQ_ADMIN || a.getRole() == AdminRole.SUPER_ADMIN)
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "사용할 수 없는 본점 계정입니다."));
    }
}
