package com.kiosk.hq.dashboard.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HqDashboardMapper {

    @Select("""
            SELECT
              COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN final_amount ELSE 0 END),0) month_sales,
              COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN 1 ELSE 0 END),0) month_count,
              COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE() - INTERVAL 1 MONTH) AND MONTH(created_at)=MONTH(CURDATE() - INTERVAL 1 MONTH) THEN final_amount ELSE 0 END),0) last_month_sales
            FROM `order`
            WHERE order_status IN ('PAID','MAKING','READY','COMPLETED')
            """)
    Map<String, Object> findMonthTotals();

    @Select("""
            SELECT o.branch_id, b.branch_name,
                   COALESCE(SUM(o.final_amount),0) sales,
                   COUNT(*) order_count
            FROM `order` o
            JOIN branch b ON b.branch_id = o.branch_id
            WHERE o.order_status IN ('PAID','MAKING','READY','COMPLETED')
              AND YEAR(o.created_at)=YEAR(CURDATE()) AND MONTH(o.created_at)=MONTH(CURDATE())
            GROUP BY o.branch_id, b.branch_name
            ORDER BY sales DESC
            """)
    List<Map<String, Object>> findBranchRanking();

    @Select("""
            SELECT f.flavor_id, f.flavor_name, COALESCE(SUM(oif.quantity),0) value
            FROM order_item_flavor oif
            JOIN order_item oi ON oi.order_item_id = oif.order_item_id
            JOIN `order` o ON o.order_id = oi.order_id
            JOIN flavor f ON f.flavor_id = oif.flavor_id
            WHERE o.order_status IN ('PAID','MAKING','READY','COMPLETED')
              AND YEAR(o.created_at)=YEAR(CURDATE()) AND MONTH(o.created_at)=MONTH(CURDATE())
            GROUP BY f.flavor_id, f.flavor_name
            ORDER BY value DESC
            LIMIT 5
            """)
    List<Map<String, Object>> findFlavorSalesRanking();

    @Select("""
            SELECT f.flavor_id, f.flavor_name, COUNT(*) value
            FROM stock_request_item sri
            JOIN stock_request sr ON sr.stock_request_id = sri.stock_request_id
            JOIN flavor f ON f.flavor_id = sri.flavor_id
            WHERE YEAR(sr.requested_at)=YEAR(CURDATE()) AND MONTH(sr.requested_at)=MONTH(CURDATE())
            GROUP BY f.flavor_id, f.flavor_name
            ORDER BY value DESC
            LIMIT 5
            """)
    List<Map<String, Object>> findFlavorRequestRanking();
}
