package com.kiosk.hq.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HqEventCreateRequest(
        String eventName,
        String eventType,
        String benefitType,
        String description,
        String imageUrl,
        BigDecimal discountRate,
        Integer discountAmount,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
