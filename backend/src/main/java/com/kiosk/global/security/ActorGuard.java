package com.kiosk.global.security;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.global.exception.BusinessException;
import com.kiosk.global.exception.ErrorCode;

/**
 * {@link CurrentAdmin}으로 주입된 관리자의 역할/소속 지점을 검증하는 공통 헬퍼.
 */
public final class ActorGuard {

    private ActorGuard() {
    }

    public static Branch requireBranchOf(Admin admin) {
        if (admin.getRole() != AdminRole.BRANCH_MANAGER || admin.getBranch() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "지점 관리자만 이용할 수 있습니다");
        }
        return admin.getBranch();
    }

    public static void requireHqRole(Admin admin) {
        if (admin.getRole() != AdminRole.HQ_ADMIN && admin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본점 관리자만 이용할 수 있습니다");
        }
    }
}
