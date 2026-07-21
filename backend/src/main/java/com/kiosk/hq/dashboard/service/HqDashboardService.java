package com.kiosk.hq.dashboard.service;

import com.kiosk.hq.dashboard.dto.HqDashboardStatsResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 전사 통계 요약. 조회 전용 집계라 HqBranchService와 동일하게 JPA 엔티티를 거치지 않고
// JdbcTemplate으로 직접 그룹핑한다. 이번달 매출/주문/객단가/전월 대비 성장률 +
// 지점별 매출 비교 + (전 지점 통합) 맛별 판매량 TOP 5 + 맛별 재고 신청 건수 TOP 5를 다룬다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqDashboardService {

    private static final String PAID_STATUSES = "('PAID','MAKING','READY','COMPLETED')";

    private final JdbcTemplate jdbcTemplate;

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
        return jdbcTemplate.queryForObject("""
                SELECT
                  COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN final_amount ELSE 0 END),0) month_sales,
                  COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN 1 ELSE 0 END),0) month_count,
                  COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE() - INTERVAL 1 MONTH) AND MONTH(created_at)=MONTH(CURDATE() - INTERVAL 1 MONTH) THEN final_amount ELSE 0 END),0) last_month_sales
                FROM `order`
                WHERE order_status IN """ + PAID_STATUSES + """
                """, (rs, rowNum) -> new long[]{rs.getLong("month_sales"), rs.getLong("month_count"), rs.getLong("last_month_sales")});
    }

    private List<HqDashboardStatsResponse.BranchRanking> branchRanking() {
        List<HqDashboardStatsResponse.BranchRanking> ranking = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT o.branch_id, b.branch_name,
                       COALESCE(SUM(o.final_amount),0) sales,
                       COUNT(*) order_count
                FROM `order` o
                JOIN branch b ON b.branch_id = o.branch_id
                WHERE o.order_status IN """ + PAID_STATUSES + """
                  AND YEAR(o.created_at)=YEAR(CURDATE()) AND MONTH(o.created_at)=MONTH(CURDATE())
                GROUP BY o.branch_id, b.branch_name
                ORDER BY sales DESC
                """, rs -> {
            ranking.add(new HqDashboardStatsResponse.BranchRanking(
                    rs.getLong("branch_id"), rs.getString("branch_name"), rs.getLong("sales"), rs.getLong("order_count")));
        });
        return ranking;
    }

    // 이번달, 전 지점 통합 기준 가장 많이 팔린 맛 TOP 5 (order_item_flavor 수량 합산).
    private List<HqDashboardStatsResponse.FlavorRanking> flavorSalesRanking() {
        List<HqDashboardStatsResponse.FlavorRanking> ranking = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT f.flavor_id, f.flavor_name, COALESCE(SUM(oif.quantity),0) qty
                FROM order_item_flavor oif
                JOIN order_item oi ON oi.order_item_id = oif.order_item_id
                JOIN `order` o ON o.order_id = oi.order_id
                JOIN flavor f ON f.flavor_id = oif.flavor_id
                WHERE o.order_status IN """ + PAID_STATUSES + """
                  AND YEAR(o.created_at)=YEAR(CURDATE()) AND MONTH(o.created_at)=MONTH(CURDATE())
                GROUP BY f.flavor_id, f.flavor_name
                ORDER BY qty DESC
                LIMIT 5
                """, rs -> {
            ranking.add(new HqDashboardStatsResponse.FlavorRanking(
                    rs.getLong("flavor_id"), rs.getString("flavor_name"), rs.getLong("qty")));
        });
        return ranking;
    }

    // 이번달 지점 재고 신청(stock_request_item)에서 어떤 맛이 가장 많이 신청됐는지 TOP 5.
    private List<HqDashboardStatsResponse.FlavorRanking> flavorRequestRanking() {
        List<HqDashboardStatsResponse.FlavorRanking> ranking = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT f.flavor_id, f.flavor_name, COUNT(*) cnt
                FROM stock_request_item sri
                JOIN stock_request sr ON sr.stock_request_id = sri.stock_request_id
                JOIN flavor f ON f.flavor_id = sri.flavor_id
                WHERE YEAR(sr.requested_at)=YEAR(CURDATE()) AND MONTH(sr.requested_at)=MONTH(CURDATE())
                GROUP BY f.flavor_id, f.flavor_name
                ORDER BY cnt DESC
                LIMIT 5
                """, rs -> {
            ranking.add(new HqDashboardStatsResponse.FlavorRanking(
                    rs.getLong("flavor_id"), rs.getString("flavor_name"), rs.getLong("cnt")));
        });
        return ranking;
    }
}
