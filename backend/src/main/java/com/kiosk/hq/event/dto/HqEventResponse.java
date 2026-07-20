package com.kiosk.hq.event.dto;

import com.kiosk.domain.event.Event;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HqEventResponse(
        Long eventId,
        String eventName,
        String eventType,
        String benefitType,
        String description,
        String imageUrl,
        BigDecimal discountRate,
        Integer discountAmount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String creatorName,
        LocalDateTime createdAt
) {
    public static HqEventResponse from(Event event) {
        return new HqEventResponse(
                event.getEventId(),
                event.getEventName(),
                event.getEventType().name(),
                event.getBenefitType().name(),
                event.getDescription(),
                event.getImageUrl(),
                event.getDiscountRate(),
                event.getDiscountAmount(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus().name(),
                event.getCreatorAdmin().getName(),
                event.getCreatedAt()
        );
    }
}
