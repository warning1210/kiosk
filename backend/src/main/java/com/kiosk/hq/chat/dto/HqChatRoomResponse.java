package com.kiosk.hq.chat.dto;

import java.time.LocalDateTime;

public record HqChatRoomResponse(
        Long chatRoomId,
        Long branchId,
        String branchName,
        String consultationStatus,
        LocalDateTime startedAt,
        String lastMessagePreview,
        LocalDateTime lastMessageAt
) {
}
