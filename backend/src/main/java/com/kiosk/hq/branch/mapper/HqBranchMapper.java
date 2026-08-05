package com.kiosk.hq.branch.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HqBranchMapper {

    @Select("""
            SELECT branch_id,
                   COALESCE(SUM(CASE WHEN DATE(created_at)=CURDATE() THEN final_amount ELSE 0 END),0) AS today,
                   COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN final_amount ELSE 0 END),0) AS month,
                   COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) THEN final_amount ELSE 0 END),0) AS year
            FROM `order`
            WHERE order_status IN ('PAID','MAKING','READY','COMPLETED')
            GROUP BY branch_id
            """)
    List<Map<String, Object>> findSalesByBranch();
}
