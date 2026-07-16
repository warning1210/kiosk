package com.kiosk.hq.branch.controller;

import com.kiosk.branch.auth.dto.ApplicationResponse;
import com.kiosk.hq.branch.dto.IssueInviteRequest;
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

    @GetMapping
    public List<ApplicationResponse> list(@RequestHeader(value = "Origin", required = false) String origin) {
        return branchApplicationService.list(origin);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse issueInvite(@RequestBody IssueInviteRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {
        return branchApplicationService.issueInvite(request.email(), origin);
    }

    @DeleteMapping("/{id}/account")
    public ApplicationResponse deleteAccount(@PathVariable Long id) {
        return branchApplicationService.deleteAccount(id);
    }

    @PostMapping("/{id}/invite")
    public ApplicationResponse regenerateInvite(@PathVariable Long id,
            @RequestHeader(value = "Origin", required = false) String origin) {
        return branchApplicationService.regenerateInvite(id, origin);
    }
}
