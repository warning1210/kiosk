package com.kiosk.kiosk.session.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branch.KioskStatus;
import com.kiosk.kiosk.session.dto.KioskDailySummaryResponse;
import com.kiosk.kiosk.session.mapper.KioskSessionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KioskSessionService {
    private final BranchRepository branchRepository;
    private final KioskSessionMapper kioskSessionMapper;

    @Transactional(readOnly = true)
    public KioskDailySummaryResponse getTodaySummary(Long branchId) {
        requireBranch(branchId);
        LocalDate today = LocalDate.now();
        Map<String,Object> summary = kioskSessionMapper.findDailySummary(branchId, today, today.plusDays(1));
        List<KioskDailySummaryResponse.PopularMenu> popularMenus =
                kioskSessionMapper.findPopularMenus(branchId, today, today.plusDays(1));
        return new KioskDailySummaryResponse(
                ((Number) summary.get("revenue")).intValue(),
                ((Number) summary.get("order_count")).intValue(),
                popularMenus);
    }

    @Transactional
    public void start(Long branchId) {
        Branch branch = requireBranch(branchId);
        branch.setKioskStatus(KioskStatus.ACTIVE);
        branch.setKioskLastAccessAt(LocalDateTime.now());
        branchRepository.update(branch);
    }

    @Transactional
    public void logout(Long branchId, boolean closeBusiness) {
        Branch branch = requireBranch(branchId);
        if (closeBusiness) {
            branch.setKioskStatus(KioskStatus.INACTIVE);
        }
        branch.setKioskLastAccessAt(LocalDateTime.now());
        branchRepository.update(branch);
    }

    private Branch requireBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."));
    }
}
