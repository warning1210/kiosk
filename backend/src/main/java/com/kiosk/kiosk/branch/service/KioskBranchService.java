package com.kiosk.kiosk.branch.service;

import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.kiosk.branch.dto.BranchStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KioskBranchService {

    private final BranchRepository branchRepository;

    public BranchStatusResponse getStatus(Long branchId) {
        return branchRepository.findById(branchId)
                .map(BranchStatusResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("지점을 찾을 수 없습니다."));
    }
}
