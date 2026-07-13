package com.kiosk.application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.domain.admin.AdminRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BranchAccessService {
    private final AdminRepository admins;

    public BranchAccessService(AdminRepository admins) {
        this.admins = admins;
    }

    public Long requireBranchId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(authorization.substring(7));
            Admin admin = admins.findByEmail(token.getEmail())
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
