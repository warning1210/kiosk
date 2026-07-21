package com.kiosk.domain.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findFirstByBranch_BranchIdAndConsultationStatusOrderByStartedAtDesc(
            Long branchId, ConsultationStatus consultationStatus);

    List<ChatRoom> findAllByOrderByStartedAtDesc();
}
