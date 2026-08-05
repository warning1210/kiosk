package com.kiosk.domain.chat;

import com.kiosk.domain.admin.AdminRole;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageRepository {
    List<ChatMessage> findByChatRoom_ChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Optional<ChatMessage> findFirstByChatRoom_ChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    // 蹂몄젏???꾩쭅 ?댁뼱蹂댁? ?딆? 吏??諛쒖떊 硫붿떆吏留??レ옄 諛곗????ы븿?쒕떎.
    long countByChatRoom_ChatRoomIdAndReadAtIsNullAndSenderAdmin_Role(Long chatRoomId, AdminRole role);
    int insert(ChatMessage message);
    int update(ChatMessage message);
    int markReadByRoomAndRole(Long chatRoomId, AdminRole role, java.time.LocalDateTime readAt);
    default ChatMessage save(ChatMessage message) { if (message.getChatMessageId() == null) insert(message); else update(message); return message; }
    default List<ChatMessage> saveAll(List<ChatMessage> values) { values.forEach(this::save); return values; }
}
