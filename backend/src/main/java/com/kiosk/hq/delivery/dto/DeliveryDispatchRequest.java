package com.kiosk.hq.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 출고 처리 요청.
 *
 * <p>자체 배송이라 운송장번호·택배사는 받지 않는다. 배송번호는 서버가 자동 발급하므로,
 * 본사 담당자는 배송담당자 이름만 직접 입력한다. 도착 예정 시각은 선택이며, 비우면 서버가
 * 기본값(며칠 뒤)을 넣는다.
 */
public record DeliveryDispatchRequest(
        @NotBlank(message = "배송담당자를 입력해주세요") String driverName,
        LocalDateTime estimatedArrivalAt
) {
}
