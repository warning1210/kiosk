package com.kiosk.kiosk.branch.dto;

import com.kiosk.domain.branch.Branch;

public record BranchStatusResponse(
        Boolean isBusy,
        Integer estimatedWaitMinutes
) {

    public static BranchStatusResponse from(Branch branch) {
        return new BranchStatusResponse(
                branch.getIsBusy(),
                branch.getEstimatedWaitMinutes() == null ? null : branch.getEstimatedWaitMinutes().intValue()
        );
    }
}
