package com.kiosk.application;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch/sales")
public class SalesStatisticsController {
    private static final String PAID = "('PAID','MAKING','READY','COMPLETED')";
    private final JdbcTemplate jdbc;
    private final BranchRepository branches;
    private final BranchAccessService branchAccess;

    public SalesStatisticsController(JdbcTemplate jdbc, BranchRepository branches, BranchAccessService branchAccess) {
        this.jdbc = jdbc;
        this.branches = branches;
        this.branchAccess = branchAccess;
    }

    @GetMapping
    public Map<String, Object> statistics(@RequestHeader(value="Authorization",required=false) String authorization) {
        Branch branch = branches.findById(branchAccess.requireBranchId(authorization)).orElseThrow();
        Long id = branch.getBranchId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", jdbc.queryForMap("""
                SELECT COALESCE(SUM(final_amount),0) revenue, COUNT(*) order_count,
                       COALESCE(ROUND(AVG(final_amount)),0) average_amount
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID, id));
        result.put("products", rows("""
                SELECT oif.flavor_name_snapshot label, SUM(oif.quantity) quantity
                FROM order_item_flavor oif JOIN order_item oi ON oi.order_item_id=oif.order_item_id
                JOIN `order` o ON o.order_id=oi.order_id
                WHERE o.branch_id=? AND o.order_status IN """ + PAID + """
                GROUP BY oif.flavor_name_snapshot ORDER BY quantity DESC LIMIT 10""", id));
        result.put("sizes", rows("""
                SELECT oi.product_name_snapshot label, SUM(oi.quantity) quantity
                FROM order_item oi JOIN `order` o ON o.order_id=oi.order_id
                WHERE o.branch_id=? AND o.order_status IN """ + PAID + """
                GROUP BY oi.product_name_snapshot ORDER BY quantity DESC""", id));
        result.put("hourly", rows("""
                SELECT CONCAT(LPAD(HOUR(created_at),2,'0'),'시') label, COUNT(*) quantity
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID + """
                GROUP BY CONCAT(LPAD(HOUR(created_at),2,'0'),'시') ORDER BY MIN(HOUR(created_at))""", id));
        result.put("monthly", rows("""
                SELECT DATE_FORMAT(created_at,'%Y-%m') label, COUNT(*) quantity, SUM(final_amount) revenue
                FROM `order` WHERE branch_id=? AND order_status IN """ + PAID + """
                GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY label""", id));
        return result;
    }

    private List<Map<String, Object>> rows(String sql, Long branchId) {
        return jdbc.queryForList(sql, branchId);
    }
}
