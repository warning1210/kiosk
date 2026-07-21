package com.kiosk.hq.chat.controller;

import com.kiosk.domain.admin.Admin;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.chat.dto.HqChatMessageResponse;
import com.kiosk.hq.chat.dto.HqChatRoomResponse;
import com.kiosk.hq.chat.dto.SendMessageRequest;
import com.kiosk.hq.chat.service.HqChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/chat")
@RequiredArgsConstructor
public class HqChatController {

    private final HqChatService hqChatService;
    private final HqAccessService hqAccessService;

    @GetMapping("/rooms")
    public List<HqChatRoomResponse> rooms(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqChatService.listRooms();
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<HqChatMessageResponse> messages(@PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqChatService.listMessages(roomId);
    }

    @PostMapping("/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public HqChatMessageResponse send(@PathVariable Long roomId, @RequestBody SendMessageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Admin admin = hqAccessService.requireAdmin(authorization);
        return hqChatService.sendMessage(roomId, admin, request.content());
    }
}
