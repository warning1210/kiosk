package com.kiosk.hq.branch.dto;

// 본점이 지점 개설 신청을 반려할 때 입력하는 사유다.
public record RejectBranchApplicationRequest(
        // reason은 예비 지점장이 확인할 구체적인 반려 이유다.
        String reason
) {
}
