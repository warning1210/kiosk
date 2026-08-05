package com.kiosk.domain.chat;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
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
public class ChatRoom {

    private Long chatRoomId;

    private Branch branch;

    @Builder.Default
    private ConsultationStatus consultationStatus = ConsultationStatus.OPEN;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Admin assignedAdmin;
}
