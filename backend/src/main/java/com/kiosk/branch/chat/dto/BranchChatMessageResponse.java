package com.kiosk.branch.chat.dto;

import java.time.LocalDateTime;

public record BranchChatMessageResponse(
        Long chatMessageId,
        Long senderAdminId,
        String senderName,
        boolean fromHq,
        String messageContent,
        LocalDateTime createdAt
) {
}
