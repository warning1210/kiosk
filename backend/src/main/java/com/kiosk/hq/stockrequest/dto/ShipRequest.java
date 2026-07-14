package com.kiosk.hq.stockrequest.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 본사가 준비된 재고 신청을 배송 처리할 때 보내는 운송 정보다.
 *
 * <p>배송을 식별하는 운송장 번호만 필수이며, 택배사·기사·도착 예정 시각은 알 수 있을
 * 때만 전달할 수 있다.</p>
 *
 * @param trackingNumber 필수 운송장 번호
 * @param courierName 선택적인 택배사 이름
 * @param driverName 선택적인 배송 기사 이름
 * @param estimatedArrivalAt 선택적인 도착 예정 시각
 */
public record ShipRequest(
        @NotBlank(message = "운송장번호를 입력해주세요") String trackingNumber,
        String courierName,
        String driverName,
        LocalDateTime estimatedArrivalAt
) {
}
