package com.kiosk.hq.branch.controller;

import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.branch.dto.IssueInviteRequest;
import com.kiosk.hq.branch.dto.RejectBranchApplicationRequest;
import com.kiosk.hq.branch.service.BranchApplicationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/branch-applications")
@RequiredArgsConstructor
public class BranchApplicationController {

    private final BranchApplicationService branchApplicationService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<ApplicationResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Origin", required = false) String origin) {
        hqAccessService.requireAdmin(authorization);
        return branchApplicationService.list(origin);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse issueInvite(@RequestBody IssueInviteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Origin", required = false) String origin) {
        hqAccessService.requireAdmin(authorization);
        return branchApplicationService.issueInvite(request.email(), origin);
    }

    @DeleteMapping("/{id}/account")
    public ApplicationResponse deleteAccount(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return branchApplicationService.deleteAccount(id);
    }

    @PostMapping("/{id}/invite")
    public ApplicationResponse regenerateInvite(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Origin", required = false) String origin) {
        hqAccessService.requireAdmin(authorization);
        return branchApplicationService.regenerateInvite(id, origin);
    }

    // 제출된 지점 개설 신청을 본점이 최종 수락한다.
    @PostMapping("/{id}/approve")
    public ApplicationResponse approve(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 로그인한 본점 관리자를 승인 처리자로 가져온다.
        Admin processor = hqAccessService.requireAdmin(authorization);
        // 지점과 지점장 계정을 활성화한다.
        return branchApplicationService.approve(id, processor);
    }

    // 제출된 지점 개설 신청을 본점이 반려한다.
    @PostMapping("/{id}/reject")
    public ApplicationResponse reject(@PathVariable Long id,
            @RequestBody RejectBranchApplicationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // 로그인한 본점 관리자를 반려 처리자로 가져온다.
        Admin processor = hqAccessService.requireAdmin(authorization);
        // 지점과 지점장 로그인을 차단하고 신청을 반려한다.
        return branchApplicationService.reject(id, request.reason(), processor);
    }
}
