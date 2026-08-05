package com.kiosk.domain.chat;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRoomRepository {
    Optional<ChatRoom> findFirstByBranch_BranchIdAndConsultationStatusOrderByStartedAtDesc(
            Long branchId, ConsultationStatus consultationStatus);

    List<ChatRoom> findAllByOrderByStartedAtDesc();
    Optional<ChatRoom> findById(Long id);
    int insert(ChatRoom room);
    int update(ChatRoom room);
    default ChatRoom save(ChatRoom room) { if (room.getChatRoomId() == null) insert(room); else update(room); return room; }
}
