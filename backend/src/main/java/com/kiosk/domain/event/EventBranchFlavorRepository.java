package com.kiosk.domain.event;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventBranchFlavorRepository {
    Optional<EventBranchFlavor> findByEvent_EventIdAndBranch_BranchId(Long eventId, Long branchId);

    List<EventBranchFlavor> findByBranch_BranchId(Long branchId);

    void deleteByEvent_EventIdAndBranch_BranchId(Long eventId, Long branchId);
    int insert(EventBranchFlavor mapping);
    default EventBranchFlavor save(EventBranchFlavor mapping) { insert(mapping); return mapping; }
}
