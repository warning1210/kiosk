package com.kiosk.branch.staffcall.service;

import com.kiosk.branch.staffcall.dto.StaffCallStatusResponse;
import com.kiosk.global.staffcall.StaffCallRegistry;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchStaffCallService {

    private final StaffCallRegistry staffCallRegistry;

    public StaffCallStatusResponse getStatus(Long branchId) {
        LocalDateTime calledAt = staffCallRegistry.getCalledAt(branchId);
        return new StaffCallStatusResponse(calledAt != null, calledAt);
    }

    public void acknowledge(Long branchId) {
        staffCallRegistry.acknowledge(branchId);
    }
}
