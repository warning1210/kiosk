package com.kiosk.domain.chat;

import com.kiosk.domain.admin.AdminRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoom_ChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Optional<ChatMessage> findFirstByChatRoom_ChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    // 본점이 아직 열어보지 않은 지점 발신 메시지만 숫자 배지에 포함한다.
    long countByChatRoom_ChatRoomIdAndReadAtIsNullAndSenderAdmin_Role(Long chatRoomId, AdminRole role);
}
