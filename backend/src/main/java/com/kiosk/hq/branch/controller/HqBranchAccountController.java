package com.kiosk.hq.branch.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.branch.dto.HqBranchAccountResponse;
import com.kiosk.hq.branch.dto.HqBranchAccountStatusRequest;
import com.kiosk.hq.branch.service.HqBranchAccountService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 본점 계정 관리 화면이 사용하는 지점장 계정 API다.
@RestController
// 계정 관리 API의 공통 주소를 지정한다.
@RequestMapping("/api/hq/branch-accounts")
// 서비스 생성자 주입 코드를 자동 생성한다.
@RequiredArgsConstructor
public class HqBranchAccountController {

    // 지점장 계정 조회와 상태 변경을 처리한다.
    private final HqBranchAccountService branchAccountService;
    // 요청자가 본점 관리자인지 검증한다.
    private final HqAccessService hqAccessService;

    // 실제로 만들어진 모든 지점장 계정을 조회한다.
    @GetMapping
    public List<HqBranchAccountResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 본점 관리자가 아니면 계정 목록 접근을 차단한다.
        hqAccessService.requireAdmin(authorization);
        // DB의 실제 지점장 계정 목록을 반환한다.
        return branchAccountService.list();
    }

    // 선택한 지점장 계정을 정상 또는 정지 상태로 바꾼다.
    @PatchMapping("/{adminId}/status")
    public HqBranchAccountResponse changeStatus(@PathVariable Long adminId,
            @RequestBody HqBranchAccountStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 본점 관리자 권한이 없으면 상태 변경을 차단한다.
        hqAccessService.requireAdmin(authorization);
        // 선택 계정 상태를 바꾸고 최신 정보를 반환한다.
        return branchAccountService.changeStatus(adminId, request.status());
    }
}
