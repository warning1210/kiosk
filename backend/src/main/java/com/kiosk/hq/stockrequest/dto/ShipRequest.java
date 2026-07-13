package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record ShipRequest(
        @NotBlank(message = "운송장번호를 입력해주세요") String trackingNumber,
        String courierName,
        String driverName,
        LocalDateTime estimatedArrivalAt
) {
}
