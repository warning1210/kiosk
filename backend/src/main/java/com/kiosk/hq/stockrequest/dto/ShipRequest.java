package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/** 배송 등록 요청. 운송장번호만 필수이고 나머지는 아는 만큼만 채운다 (HQ-013). */
public record ShipRequest(
        @NotBlank(message = "운송장번호를 입력해주세요") String trackingNumber,
        String courierName,
        String driverName,
        LocalDateTime estimatedArrivalAt
) {
}
