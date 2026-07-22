package com.kiosk.branch.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BranchEventResponse(
        Long eventId,
        String eventName,
        String eventType,
        String benefitType,
        BigDecimal discountRate,
        Integer discountAmount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long selectedFlavorId,
        String selectedFlavorName,
        List<FlavorOptionResponse> flavorOptions
) {
}
