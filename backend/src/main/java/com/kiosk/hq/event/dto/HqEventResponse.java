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
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getBenefitType() != null ? event.getBenefitType().name() : null,
                event.getDescription(),
                event.getImageUrl(),
                event.getDiscountRate(),
                event.getDiscountAmount(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus() != null ? event.getStatus().name() : null,
                event.getCreatorAdmin() != null ? event.getCreatorAdmin().getName() : null,
                event.getCreatedAt()
        );
    }
}
