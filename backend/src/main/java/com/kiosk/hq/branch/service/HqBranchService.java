package com.kiosk.hq.branch.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.hq.branch.dto.HqBranchStatusResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HQ-대시보드: 전 지점 상태 + 일/월/년 매출. 조회 전용 집계라 BranchSalesService와 동일하게
// JPA 엔티티를 거치지 않고 JdbcTemplate으로 직접 그룹핑한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqBranchService {

    private static final String PAID_STATUSES = "('PAID','MAKING','READY','COMPLETED')";

    private final BranchRepository branchRepository;
    private final JdbcTemplate jdbcTemplate;

    public java.util.List<HqBranchStatusResponse> listWithSales() {
        Map<Long, long[]> salesByBranch = salesByBranch();
        return branchRepository.findAll().stream()
                .sorted(Comparator.comparing(Branch::getBranchId))
                .map(branch -> toResponse(branch, salesByBranch.getOrDefault(branch.getBranchId(), new long[]{0, 0, 0})))
                .toList();
    }

    private Map<Long, long[]> salesByBranch() {
        Map<Long, long[]> result = new HashMap<>();
        jdbcTemplate.query("""
                SELECT branch_id,
                       COALESCE(SUM(CASE WHEN DATE(created_at)=CURDATE() THEN final_amount ELSE 0 END),0) today,
                       COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE()) THEN final_amount ELSE 0 END),0) month,
                       COALESCE(SUM(CASE WHEN YEAR(created_at)=YEAR(CURDATE()) THEN final_amount ELSE 0 END),0) year
                FROM `order`
                WHERE order_status IN """ + PAID_STATUSES + """
                GROUP BY branch_id
                """, rs -> {
            result.put(rs.getLong("branch_id"), new long[]{rs.getLong("today"), rs.getLong("month"), rs.getLong("year")});
        });
        return result;
    }

    private HqBranchStatusResponse toResponse(Branch branch, long[] sales) {
        return new HqBranchStatusResponse(
                branch.getBranchId(),
                branch.getBranchName(),
                branch.getRegion(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getManagerName(),
                branch.getOperationStatus().name(),
                branch.getKioskStatus().name(),
                branch.getIsBusy(),
                branch.getEstimatedWaitMinutes(),
                branch.getKioskLastAccessAt(),
                sales[0], sales[1], sales[2]
        );
    }
}
