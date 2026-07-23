package com.kiosk.hq.notice.controller;

import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.notice.dto.HqNoticeResponse;
import com.kiosk.hq.notice.dto.HqNoticeUpsertRequest;
import com.kiosk.hq.notice.service.HqNoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/notices")
@RequiredArgsConstructor
public class HqNoticeController {

    private final HqNoticeService hqNoticeService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqNoticeResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqNoticeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HqNoticeResponse create(@RequestBody HqNoticeUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqNoticeService.create(request, admin);
    }

    @PutMapping("/{noticeId}")
    public HqNoticeResponse update(@PathVariable Long noticeId, @RequestBody HqNoticeUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqNoticeService.update(noticeId, request);
    }
}
