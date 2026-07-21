package com.kiosk.branch.status.service;

import com.kiosk.branch.status.dto.BranchStatusResponse;
import com.kiosk.branch.status.dto.BranchStatusUpdateRequest;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchStatusService {

    // 지점 관리자가 고를 수 있는 대기시간 옵션 - 프론트 팝업(10/20/30분)과 반드시 일치해야 한다.
    private static final Set<Integer> ALLOWED_WAIT_MINUTES = Set.of(10, 20, 30);

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public BranchStatusResponse getStatus(Long branchId) {
        return toResponse(findBranch(branchId));
    }

    @Transactional
    public BranchStatusResponse updateStatus(Long branchId, BranchStatusUpdateRequest request) {
        Branch branch = findBranch(branchId);

        if (Boolean.TRUE.equals(request.getIsBusy())) {
            if (request.getEstimatedWaitMinutes() == null || !ALLOWED_WAIT_MINUTES.contains(request.getEstimatedWaitMinutes())) {
                throw new IllegalArgumentException("대기시간은 10분, 20분, 30분 중 하나여야 합니다.");
            }
            branch.setIsBusy(true);
            branch.setEstimatedWaitMinutes(request.getEstimatedWaitMinutes().byteValue());
        } else {
            branch.setIsBusy(false);
            branch.setEstimatedWaitMinutes(null);
        }

        return toResponse(branch);
    }

    private Branch findBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("지점을 찾을 수 없습니다."));
    }

    private BranchStatusResponse toResponse(Branch branch) {
        return BranchStatusResponse.builder()
                .isBusy(branch.getIsBusy())
                .estimatedWaitMinutes(branch.getEstimatedWaitMinutes() == null ? null : branch.getEstimatedWaitMinutes().intValue())
                .build();
    }
}
