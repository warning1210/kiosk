package com.kiosk.domain.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventBranchFlavorRepository extends JpaRepository<EventBranchFlavor, Long> {
    Optional<EventBranchFlavor> findByEvent_EventIdAndBranch_BranchId(Long eventId, Long branchId);

    List<EventBranchFlavor> findByBranch_BranchId(Long branchId);

    void deleteByEvent_EventIdAndBranch_BranchId(Long eventId, Long branchId);
}
