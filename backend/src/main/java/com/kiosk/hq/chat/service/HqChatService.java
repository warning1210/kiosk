package com.kiosk.hq.chat.service;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.chat.ChatMessage;
import com.kiosk.domain.chat.ChatMessageRepository;
import com.kiosk.domain.chat.ChatRoom;
import com.kiosk.domain.chat.ChatRoomRepository;
import com.kiosk.hq.chat.dto.HqChatMessageResponse;
import com.kiosk.hq.chat.dto.HqChatRoomResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 지점↔본점 채팅 - 본점 쪽. 전 지점의 상담방을 한눈에 보고, 미배정 방에 답장하면 자동으로 담당자가 된다.
@Service
@RequiredArgsConstructor
@Transactional
public class HqChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<HqChatRoomResponse> listRooms() {
        return chatRoomRepository.findAllByOrderByStartedAtDesc().stream()
                .map(room -> {
                    ChatMessage last = chatMessageRepository
                            .findFirstByChatRoom_ChatRoomIdOrderByCreatedAtDesc(room.getChatRoomId())
                            .orElse(null);
                    return new HqChatRoomResponse(
                            room.getChatRoomId(),
                            room.getBranch().getBranchId(),
                            room.getBranch().getBranchName(),
                            room.getConsultationStatus().name(),
                            room.getStartedAt(),
                            last != null ? last.getMessageContent() : null,
                            last != null ? last.getCreatedAt() : null,
                            chatMessageRepository.countByChatRoom_ChatRoomIdAndReadAtIsNullAndSenderAdmin_Role(
                                    room.getChatRoomId(), AdminRole.BRANCH_MANAGER)
                    );
                })
                .toList();
    }

    public List<HqChatMessageResponse> listMessages(Long chatRoomId) {
        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoom_ChatRoomIdOrderByCreatedAtAsc(chatRoomId);
        // 본점이 상담방을 열면 현재 보이는 지점 메시지를 읽음으로 저장해 배지를 즉시 줄인다.
        java.time.LocalDateTime readAt = java.time.LocalDateTime.now();
        messages.stream()
                .filter(message -> message.getSenderAdmin().getRole() == AdminRole.BRANCH_MANAGER)
                .filter(message -> message.getReadAt() == null)
                .forEach(message -> message.setReadAt(readAt));
        chatMessageRepository.saveAll(messages);
        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    public HqChatMessageResponse sendMessage(Long chatRoomId, Admin admin, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
        }
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (room.getAssignedAdmin() == null) {
            room.setAssignedAdmin(admin);
            chatRoomRepository.save(room);
        }
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderAdmin(admin)
                .messageContent(content)
                .build());
        return toResponse(message);
    }

    private HqChatMessageResponse toResponse(ChatMessage message) {
        Admin sender = message.getSenderAdmin();
        boolean isBranchManager = sender.getRole() == AdminRole.BRANCH_MANAGER;
        return new HqChatMessageResponse(
                message.getChatMessageId(),
                sender.getAdminId(),
                isBranchManager ? sender.getBranch().getBranchName() : sender.getName(),
                !isBranchManager,
                message.getMessageContent(),
                message.getCreatedAt()
        );
    }
}
