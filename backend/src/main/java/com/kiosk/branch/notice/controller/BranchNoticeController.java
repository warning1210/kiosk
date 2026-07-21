package com.kiosk.branch.notice.controller;

import com.kiosk.branch.notice.dto.BranchNoticeResponse;
import com.kiosk.branch.notice.service.BranchNoticeService;
import com.kiosk.global.security.BranchAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/notices")
@RequiredArgsConstructor
public class BranchNoticeController {

    private final BranchNoticeService branchNoticeService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public List<BranchNoticeResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        branchAccessService.requireBranchId(authorization);
        return branchNoticeService.list();
    }
}
