package com.kiosk.hq.branch.dto;

import java.time.LocalDateTime;

public record HqBranchStatusResponse(
        Long branchId,
        String branchName,
        String region,
        String address,
        String phone,
        String managerName,
        String operationStatus,
        String kioskStatus,
        Boolean isBusy,
        Byte estimatedWaitMinutes,
        LocalDateTime kioskLastAccessAt,
        long salesToday,
        long salesMonth,
        long salesYear
) {
}
