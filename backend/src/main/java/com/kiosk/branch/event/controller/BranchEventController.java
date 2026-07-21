package com.kiosk.branch.event.controller;

import com.kiosk.branch.event.dto.BranchEventResponse;
import com.kiosk.branch.event.dto.SelectFlavorRequest;
import com.kiosk.branch.event.service.BranchEventService;
import com.kiosk.global.security.BranchAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/events")
@RequiredArgsConstructor
public class BranchEventController {

    private final BranchEventService branchEventService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public List<BranchEventResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return branchEventService.list(branchAccessService.requireBranchId(authorization));
    }

    @PostMapping("/{eventId}/flavor")
    public BranchEventResponse selectFlavor(@PathVariable Long eventId, @RequestBody SelectFlavorRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return branchEventService.selectFlavor(branchAccessService.requireBranchId(authorization), eventId, request);
    }
}
