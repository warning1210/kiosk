package com.kiosk.hq.chat.dto;

import java.time.LocalDateTime;

public record HqChatMessageResponse(
        Long chatMessageId,
        Long senderAdminId,
        String senderName,
        boolean fromHq,
        String messageContent,
        LocalDateTime createdAt
) {
}
