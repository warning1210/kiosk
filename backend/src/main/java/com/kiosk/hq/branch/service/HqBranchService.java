package com.kiosk.hq.branch.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.hq.branch.dto.HqBranchStatusResponse;
import com.kiosk.hq.branch.mapper.HqBranchMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HQ-대시보드: 전 지점 상태 + 일/월/년 매출. 조회 전용 집계라 BranchSalesService와 동일하게
// 본사 지점 현황 조회 서비스.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqBranchService {

    private final BranchRepository branchRepository;
    private final HqBranchMapper hqBranchMapper;

    public java.util.List<HqBranchStatusResponse> listWithSales() {
        Map<Long, long[]> salesByBranch = salesByBranch();
        return branchRepository.findAll().stream()
                .sorted(Comparator.comparing(Branch::getBranchId))
                .map(branch -> toResponse(branch, salesByBranch.getOrDefault(branch.getBranchId(), new long[]{0, 0, 0})))
                .toList();
    }

    private Map<Long, long[]> salesByBranch() {
        Map<Long, long[]> result = new HashMap<>();
        hqBranchMapper.findSalesByBranch().forEach(row -> result.put(
                ((Number) row.get("branch_id")).longValue(),
                new long[]{number(row, "today"), number(row, "month"), number(row, "year")}));
        return result;
    }

    private long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0L : ((Number) value).longValue();
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
