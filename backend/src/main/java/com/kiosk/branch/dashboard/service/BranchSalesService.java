package com.kiosk.branch.dashboard.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// BR-006/007/008: 지점 매출/인기맛/사이즈별/시간대별/월별 통계. 조회 전용이라 JPA 엔티티를 거치지 않고
// 직접 SQL로 집계한다 (엔티티로 표현하기엔 그룹핑/집계 위주라 오히려 더 장황해짐).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchSalesService {

    private static final String PAID_STATUSES = "('PAID','MAKING','READY','COMPLETED')";

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getStatistics(Long branchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(final_amount),0) revenue, COUNT(*) order_count,
                       COALESCE(ROUND(AVG(final_amount)),0) average_amount
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID_STATUSES, branchId));
        result.put("flavors", rows("""
                SELECT oif.flavor_name_snapshot label, SUM(oif.quantity) quantity
                FROM order_item_flavor oif JOIN order_item oi ON oi.order_item_id=oif.order_item_id
                JOIN `order` o ON o.order_id=oi.order_id
                WHERE o.branch_id=? AND o.order_status IN """ + PAID_STATUSES + """
                GROUP BY oif.flavor_name_snapshot ORDER BY quantity DESC LIMIT 10""", branchId));
        result.put("sizes", rows("""
                SELECT oi.product_name_snapshot label, SUM(oi.quantity) quantity
                FROM order_item oi JOIN `order` o ON o.order_id=oi.order_id
                WHERE o.branch_id=? AND o.order_status IN """ + PAID_STATUSES + """
                GROUP BY oi.product_name_snapshot ORDER BY quantity DESC""", branchId));
        result.put("hourly", rows("""
                SELECT CONCAT(LPAD(HOUR(created_at),2,'0'),'시') label, COUNT(*) quantity
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID_STATUSES + """
                GROUP BY CONCAT(LPAD(HOUR(created_at),2,'0'),'시') ORDER BY MIN(HOUR(created_at))""", branchId));
        result.put("monthly", rows("""
                SELECT DATE_FORMAT(created_at,'%Y-%m') label, COUNT(*) quantity, SUM(final_amount) revenue
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID_STATUSES + """
                GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY label""", branchId));
        return result;
    }

    private List<Map<String, Object>> rows(String sql, Long branchId) {
        return jdbcTemplate.queryForList(sql, branchId);
    }
}
