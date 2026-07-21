package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventType;
import com.kiosk.domain.flavor.Flavor;
import java.math.BigDecimal;

public record FlavorResponse(
        Long flavorId,
        String flavorName,
        String imageUrl,
        String description,
        String allergyInfo,
        String discountType,
        BigDecimal discountRate,
        Integer discountAmount,
        Boolean isMonthly
) {

    public static FlavorResponse from(Flavor flavor) {
        return from(flavor, null);
    }

    public static FlavorResponse from(Flavor flavor, Event discountEvent) {
        return new FlavorResponse(
                flavor.getFlavorId(),
                flavor.getFlavorName(),
                flavor.getImageUrl(),
                flavor.getDescription(),
                flavor.getAllergyInfo(),
                discountEvent != null ? discountEvent.getBenefitType().name() : null,
                discountEvent != null && discountEvent.getBenefitType() == BenefitType.DISCOUNT_RATE ? discountEvent.getDiscountRate() : null,
                discountEvent != null && discountEvent.getBenefitType() == BenefitType.DISCOUNT_AMOUNT ? discountEvent.getDiscountAmount() : null,
                discountEvent != null && discountEvent.getEventType() == EventType.MONTHLY_FLAVOR
        );
    }
}
