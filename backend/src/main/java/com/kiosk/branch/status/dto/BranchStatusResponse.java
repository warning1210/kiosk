package com.kiosk.branch.status.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchStatusResponse {
    private Boolean isBusy;
    private Integer estimatedWaitMinutes;
}
