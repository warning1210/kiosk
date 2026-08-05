package com.kiosk.branch.dashboard.service;

import com.kiosk.branch.dashboard.mapper.BranchSalesMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchSalesService {
    private final BranchSalesMapper branchSalesMapper;

    public Map<String,Object> getStatistics(Long branchId) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("summary", branchSalesMapper.findSummary(branchId));
        result.put("flavors", branchSalesMapper.findFlavorSales(branchId));
        result.put("sizes", branchSalesMapper.findSizeSales(branchId));
        result.put("hourly", branchSalesMapper.findHourlySales(branchId));
        result.put("monthly", branchSalesMapper.findMonthlySales(branchId));
        return result;
    }
}
