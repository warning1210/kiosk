package com.kiosk.branch.chat.service;

import com.kiosk.branch.chat.dto.BranchChatMessageResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.chat.ChatMessage;
import com.kiosk.domain.chat.ChatMessageRepository;
import com.kiosk.domain.chat.ChatRoom;
import com.kiosk.domain.chat.ChatRoomRepository;
import com.kiosk.domain.chat.ConsultationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지점↔본점 채팅 - 지점 쪽. 지점은 항상 자신의 OPEN 상담방 하나만 본다(없으면 메시지를 처음 보낼 때 생성).
@Service
@RequiredArgsConstructor
@Transactional
public class BranchChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<BranchChatMessageResponse> listMessages(Admin admin) {
        return openRoom(admin.getBranch().getBranchId())
                .map(room -> chatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtAsc(room.getChatRoomId()).stream()
                        .map(this::toResponse)
                        .toList())
                .orElse(List.of());
    }

    public BranchChatMessageResponse sendMessage(Admin admin, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
        }
        ChatRoom room = getOrCreateOpenRoom(admin.getBranch());
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderAdmin(admin)
                .messageContent(content)
                .build());
        return toResponse(message);
    }

    private ChatRoom getOrCreateOpenRoom(Branch branch) {
        return openRoom(branch.getBranchId())
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .branch(branch)
                        .consultationStatus(ConsultationStatus.OPEN)
                        .startedAt(LocalDateTime.now())
                        .build()));
    }

    private java.util.Optional<ChatRoom> openRoom(Long branchId) {
        return chatRoomRepository.findFirstByBranch_BranchIdAndConsultationStatusOrderByStartedAtDesc(branchId, ConsultationStatus.OPEN);
    }

    private BranchChatMessageResponse toResponse(ChatMessage message) {
        Admin sender = message.getSenderAdmin();
        return new BranchChatMessageResponse(
                message.getChatMessageId(),
                sender.getAdminId(),
                sender.getName(),
                sender.getRole() != AdminRole.BRANCH_MANAGER,
                message.getMessageContent(),
                message.getCreatedAt()
        );
    }
}
