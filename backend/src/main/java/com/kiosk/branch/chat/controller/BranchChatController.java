package com.kiosk.branch.chat.controller;

import com.kiosk.branch.chat.dto.BranchChatMessageResponse;
import com.kiosk.branch.chat.dto.SendMessageRequest;
import com.kiosk.branch.chat.service.BranchChatService;
import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.BranchAccessService;
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
@RequestMapping("/api/branch/chat")
@RequiredArgsConstructor
public class BranchChatController {

    private final BranchChatService branchChatService;
    private final BranchAccessService branchAccessService;

    @GetMapping("/messages")
    public List<BranchChatMessageResponse> messages(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return branchChatService.listMessages(admin);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchChatMessageResponse send(@RequestBody SendMessageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = branchAccessService.requireAdmin(authorization);
        return branchChatService.sendMessage(admin, request.content());
    }
}
