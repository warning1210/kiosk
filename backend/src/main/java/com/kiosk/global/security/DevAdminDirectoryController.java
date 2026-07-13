package com.kiosk.global.security;

import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.AdminRepository;
import com.kiosk.global.response.ApiResponse;
import com.kiosk.global.security.dto.AdminSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 임시 스캐폴딩: 실제 로그인(hq.auth / branch.auth)이 구현되기 전까지, 프론트에서
 * "현재 로그인한 관리자"를 흉내내기 위해 선택 가능한 관리자 목록을 내려준다.
 * 실제 인증이 도입되면 이 컨트롤러는 제거한다.
 */
@RestController
public class DevAdminDirectoryController {

    private final AdminRepository adminRepository;

    public DevAdminDirectoryController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @GetMapping("/api/admins")
    public ApiResponse<List<AdminSummaryResponse>> list() {
        return ApiResponse.ok(adminRepository.findByAccountStatusOrderByRoleAscNameAsc(AccountStatus.ACTIVE).stream()
                .map(AdminSummaryResponse::from)
                .toList());
    }
}
