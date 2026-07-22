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
        Boolean isMonthly,
        Long sizeUpFromProductId,
        Long sizeUpToProductId,
        String sizeUpToProductName,
        Integer sizeUpAdditionalPayment
) {

    public static FlavorResponse from(Flavor flavor) {
        return from(flavor, null);
    }

    // discountEvent가 MONTHLY_FLAVOR이면서 사이즈업 정보(sizeUpFromProduct)까지 갖고 있으면, 이 맛을
    // 고를 때 상품 사이즈업도 같이 제안할 수 있다 (실제 배스킨라빈스식 "이 맛 고르면 사이즈업" 프로모션).
    // MONTHLY_FLAVOR는 할인이 없어 benefitType이 null일 수 있다 - discountType도 그때는 null로 남는다.
    public static FlavorResponse from(Flavor flavor, Event discountEvent) {
        boolean isMonthly = discountEvent != null && discountEvent.getEventType() == EventType.MONTHLY_FLAVOR;
        boolean hasSizeUp = isMonthly && discountEvent.getSizeUpFromProduct() != null;
        BenefitType benefitType = discountEvent != null ? discountEvent.getBenefitType() : null;
        return new FlavorResponse(
                flavor.getFlavorId(),
                flavor.getFlavorName(),
                flavor.getImageUrl(),
                flavor.getDescription(),
                flavor.getAllergyInfo(),
                benefitType != null ? benefitType.name() : null,
                benefitType == BenefitType.DISCOUNT_RATE ? discountEvent.getDiscountRate() : null,
                benefitType == BenefitType.DISCOUNT_AMOUNT ? discountEvent.getDiscountAmount() : null,
                isMonthly,
                hasSizeUp ? discountEvent.getSizeUpFromProduct().getProductId() : null,
                hasSizeUp ? discountEvent.getSizeUpToProduct().getProductId() : null,
                hasSizeUp ? discountEvent.getSizeUpToProduct().getProductName() : null,
                hasSizeUp ? discountEvent.getAdditionalPayment() : null
        );
    }
}
