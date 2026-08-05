package com.kiosk.branch.dashboard.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BranchSalesMapper {
    @Select("SELECT COALESCE(SUM(final_amount),0) revenue, COUNT(*) order_count, COALESCE(ROUND(AVG(final_amount)),0) average_amount FROM `order` WHERE branch_id=#{branchId} AND order_status IN ('PAID','MAKING','READY','COMPLETED')")
    Map<String,Object> findSummary(@Param("branchId") Long branchId);
    @Select("SELECT oif.flavor_name_snapshot label, SUM(oif.quantity) quantity FROM order_item_flavor oif JOIN order_item oi ON oi.order_item_id=oif.order_item_id JOIN `order` o ON o.order_id=oi.order_id WHERE o.branch_id=#{branchId} AND o.order_status IN ('PAID','MAKING','READY','COMPLETED') GROUP BY oif.flavor_name_snapshot ORDER BY quantity DESC LIMIT 10")
    List<Map<String,Object>> findFlavorSales(@Param("branchId") Long branchId);
    @Select("SELECT oi.product_name_snapshot label, SUM(oi.quantity) quantity FROM order_item oi JOIN `order` o ON o.order_id=oi.order_id WHERE o.branch_id=#{branchId} AND o.order_status IN ('PAID','MAKING','READY','COMPLETED') GROUP BY oi.product_name_snapshot ORDER BY quantity DESC")
    List<Map<String,Object>> findSizeSales(@Param("branchId") Long branchId);
    @Select("SELECT CONCAT(LPAD(HOUR(created_at),2,'0'),'시') label, COUNT(*) quantity FROM `order` WHERE branch_id=#{branchId} AND order_status IN ('PAID','MAKING','READY','COMPLETED') GROUP BY CONCAT(LPAD(HOUR(created_at),2,'0'),'시') ORDER BY MIN(HOUR(created_at))")
    List<Map<String,Object>> findHourlySales(@Param("branchId") Long branchId);
    @Select("SELECT DATE_FORMAT(created_at,'%Y-%m') label, COUNT(*) quantity, SUM(final_amount) revenue FROM `order` WHERE branch_id=#{branchId} AND order_status IN ('PAID','MAKING','READY','COMPLETED') GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY label")
    List<Map<String,Object>> findMonthlySales(@Param("branchId") Long branchId);
}
