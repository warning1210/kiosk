package com.kiosk.branch.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchDashboardSummaryDto {
    private long newOrderCount;
    private long processingOrderCount;
    private long completedOrderCount;
    private long todaySalesAmount;
}
