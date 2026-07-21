package com.kiosk.hq.branch.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 본점 계정 관리 표 한 행에 필요한 지점장 계정 정보다.
public record HqBranchAccountResponse(
        // adminId는 계정 상태 변경에 사용할 관리자 식별자다.
        Long adminId,
        // loginId는 지점장이 로그인할 때 사용하는 아이디다.
        String loginId,
        // branchId는 계정이 연결된 지점을 구분한다.
        Long branchId,
        // branchName은 계정이 속한 지점명이다.
        String branchName,
        // managerName은 지점장 이름이다.
        String managerName,
        // phone은 지점장 연락처다.
        String phone,
        // email은 본점이 초대 URL을 보낸 이메일이다.
        String email,
        // lastLoginAt은 지점장의 마지막 로그인 시각이다.
        LocalDateTime lastLoginAt,
        // accountStatus는 정상·정지·삭제 상태를 나타낸다.
        String accountStatus,
        // createdAt은 계정이 처음 만들어진 시각이다.
        LocalDateTime createdAt,
        // 계정 권한이며 미연결 지점은 값이 없다.
        String role,
        // 계정 정보가 마지막으로 수정된 시각이다.
        LocalDateTime accountUpdatedAt,
        // 아래 값들은 branch 테이블에 저장된 지점 상세 정보다.
        String region,
        String address,
        String branchPhone,
        String branchEmail,
        String operationStatus,
        LocalDate openingDate,
        Boolean busy,
        Byte estimatedWaitMinutes,
        String kioskCode,
        String kioskStatus,
        LocalDateTime kioskLastAccessAt,
        LocalDateTime branchCreatedAt,
        LocalDateTime branchUpdatedAt,
        // online은 최근 지점 신호가 기준 시간 안에 들어왔는지 나타낸다.
        Boolean online
) {
}
