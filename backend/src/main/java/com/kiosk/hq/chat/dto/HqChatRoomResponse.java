package com.kiosk.hq.chat.dto;

import java.time.LocalDateTime;

public record HqChatRoomResponse(
        Long chatRoomId,
        Long branchId,
        String branchName,
        String consultationStatus,
        LocalDateTime startedAt,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        // unreadCount는 해당 상담방에서 본점이 아직 확인하지 않은 지점 메시지 수다.
        long unreadCount
) {
}
