package com.kiosk.domain.chat;

import com.kiosk.domain.admin.Admin;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    private Long chatMessageId;

    private ChatRoom chatRoom;

    private Admin senderAdmin;

    private String messageContent;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
