package com.kiosk.hq.event.controller;

import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.event.dto.HqEventCreateRequest;
import com.kiosk.hq.event.dto.HqEventResponse;
import com.kiosk.hq.event.service.HqEventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/events")
@RequiredArgsConstructor
public class HqEventController {

    private final HqEventService hqEventService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqEventResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqEventService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HqEventResponse create(@RequestBody HqEventCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqEventService.create(request, admin);
    }
}
