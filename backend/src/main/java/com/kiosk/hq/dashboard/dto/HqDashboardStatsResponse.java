package com.kiosk.hq.dashboard.dto;

import java.util.List;

public record HqDashboardStatsResponse(
        long monthSales,
        long monthOrderCount,
        long avgOrderValue,
        Double growthRatePercent,
        List<BranchRanking> branchRanking,
        List<FlavorRanking> flavorSalesRanking,
        List<FlavorRanking> flavorRequestRanking
) {
    public record BranchRanking(Long branchId, String branchName, long sales, long orderCount) {
    }

    public record FlavorRanking(Long flavorId, String flavorName, long value) {
    }
}
