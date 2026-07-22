package com.kiosk.branch.status.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BranchStatusUpdateRequest {
    private Boolean isBusy;
    private Integer estimatedWaitMinutes;
}
