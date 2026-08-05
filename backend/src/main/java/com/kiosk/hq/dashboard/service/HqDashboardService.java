package com.kiosk.hq.dashboard.service;

import com.kiosk.hq.dashboard.dto.HqDashboardStatsResponse;
import com.kiosk.hq.dashboard.mapper.HqDashboardMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 전사 통계 요약.
// MyBatis Mapper로 직접 그룹핑한다. 이번달 매출/주문/객단가/전월 대비 성장률 +
// 지점별 매출 비교 + (전 지점 통합) 맛별 판매량 TOP 5 + 맛별 재고 신청 건수 TOP 5를 다룬다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqDashboardService {

    private final HqDashboardMapper hqDashboardMapper;

    public HqDashboardStatsResponse getStats() {
        long[] totals = monthTotals();
        long monthSales = totals[0];
        long monthOrderCount = totals[1];
        long lastMonthSales = totals[2];
        long avgOrderValue = monthOrderCount == 0 ? 0 : Math.round((double) monthSales / monthOrderCount);
        Double growthRatePercent = lastMonthSales == 0 ? null
                : Math.round((monthSales - lastMonthSales) * 1000.0 / lastMonthSales) / 10.0;

        return new HqDashboardStatsResponse(monthSales, monthOrderCount, avgOrderValue, growthRatePercent,
                branchRanking(), flavorSalesRanking(), flavorRequestRanking());
    }

    private long[] monthTotals() {
        Map<String, Object> row = hqDashboardMapper.findMonthTotals();
        return new long[]{number(row, "month_sales"), number(row, "month_count"), number(row, "last_month_sales")};
    }

    private List<HqDashboardStatsResponse.BranchRanking> branchRanking() {
        return hqDashboardMapper.findBranchRanking().stream()
                .map(row -> new HqDashboardStatsResponse.BranchRanking(
                        number(row, "branch_id"), (String) row.get("branch_name"),
                        number(row, "sales"), number(row, "order_count")))
                .toList();
    }

    // 이번달, 전 지점 통합 기준 가장 많이 팔린 맛 TOP 5 (order_item_flavor 수량 합산).
    private List<HqDashboardStatsResponse.FlavorRanking> flavorSalesRanking() {
        return flavorRanking(hqDashboardMapper.findFlavorSalesRanking());
    }

    // 이번달 지점 재고 신청(stock_request_item)에서 어떤 맛이 가장 많이 신청됐는지 TOP 5.
    private List<HqDashboardStatsResponse.FlavorRanking> flavorRequestRanking() {
        return flavorRanking(hqDashboardMapper.findFlavorRequestRanking());
    }

    private List<HqDashboardStatsResponse.FlavorRanking> flavorRanking(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new HqDashboardStatsResponse.FlavorRanking(
                        number(row, "flavor_id"), (String) row.get("flavor_name"), number(row, "value")))
                .toList();
    }

    private long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0L : ((Number) value).longValue();
    }
}
