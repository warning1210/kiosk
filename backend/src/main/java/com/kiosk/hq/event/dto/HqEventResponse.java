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
        Long flavorId,
        String flavorName,
        Long sizeUpFromProductId,
        String sizeUpFromProductName,
        Long sizeUpToProductId,
        String sizeUpToProductName,
        Integer additionalPayment,
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
                event.getFlavor() != null ? event.getFlavor().getFlavorId() : null,
                event.getFlavor() != null ? event.getFlavor().getFlavorName() : null,
                event.getSizeUpFromProduct() != null ? event.getSizeUpFromProduct().getProductId() : null,
                event.getSizeUpFromProduct() != null ? event.getSizeUpFromProduct().getProductName() : null,
                event.getSizeUpToProduct() != null ? event.getSizeUpToProduct().getProductId() : null,
                event.getSizeUpToProduct() != null ? event.getSizeUpToProduct().getProductName() : null,
                event.getAdditionalPayment(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus() != null ? event.getStatus().name() : null,
                event.getCreatorAdmin() != null ? event.getCreatorAdmin().getName() : null,
                event.getCreatedAt()
        );
    }
}
