package com.kiosk.domain.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoom_ChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Optional<ChatMessage> findFirstByChatRoom_ChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
