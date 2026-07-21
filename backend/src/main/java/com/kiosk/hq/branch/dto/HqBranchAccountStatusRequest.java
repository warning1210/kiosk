package com.kiosk.hq.branch.dto;

// 본점이 지점장 계정 상태를 변경할 때 보내는 요청이다.
public record HqBranchAccountStatusRequest(
        // status는 ACTIVE 또는 SUSPENDED 값이다.
        String status
) {
}
