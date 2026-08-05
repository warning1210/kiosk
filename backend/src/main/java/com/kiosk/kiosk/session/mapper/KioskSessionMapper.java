package com.kiosk.kiosk.session.mapper;

import com.kiosk.kiosk.session.dto.KioskDailySummaryResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;

@Mapper
public interface KioskSessionMapper {
    @Select("SELECT COALESCE(SUM(final_amount),0) revenue, COUNT(*) order_count FROM `order` WHERE branch_id=#{branchId} AND order_status IN ('PAID','MAKING','READY','COMPLETED') AND created_at >= #{fromDate} AND created_at < #{toDate}")
    Map<String,Object> findDailySummary(@Param("branchId") Long branchId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @ConstructorArgs({
        @Arg(column = "product_name", javaType = String.class),
        @Arg(column = "quantity", javaType = int.class)
    })
    @Select("""
        SELECT sold_name product_name, SUM(quantity) quantity FROM (
          SELECT oif.flavor_name_snapshot sold_name, SUM(oif.quantity) quantity
          FROM order_item_flavor oif JOIN order_item oi ON oi.order_item_id=oif.order_item_id JOIN `order` o ON o.order_id=oi.order_id
          WHERE o.branch_id=#{branchId} AND o.order_status IN ('PAID','MAKING','READY','COMPLETED') AND o.created_at >= #{fromDate} AND o.created_at < #{toDate}
          GROUP BY oif.flavor_name_snapshot
          UNION ALL
          SELECT oi.product_name_snapshot sold_name, SUM(oi.quantity) quantity
          FROM order_item oi JOIN `order` o ON o.order_id=oi.order_id
          WHERE o.branch_id=#{branchId} AND o.order_status IN ('PAID','MAKING','READY','COMPLETED') AND o.created_at >= #{fromDate} AND o.created_at < #{toDate}
            AND NOT EXISTS (SELECT 1 FROM order_item_flavor oif WHERE oif.order_item_id=oi.order_item_id)
          GROUP BY oi.product_name_snapshot
        ) sold GROUP BY sold_name ORDER BY quantity DESC, product_name ASC LIMIT 5
        """)
    List<KioskDailySummaryResponse.PopularMenu> findPopularMenus(@Param("branchId") Long branchId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
