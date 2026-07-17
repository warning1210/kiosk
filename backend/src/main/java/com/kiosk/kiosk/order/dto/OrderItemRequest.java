package com.kiosk.kiosk.order.dto;

import com.kiosk.domain.order.ContainerType;
import java.util.List;

public record OrderItemRequest(
        Long productId,
        ContainerType containerType,
        Integer spoonCount,
        Integer dryIceMinutes,
        List<Long> flavorIds,
        Boolean monthlyFlavorUpgrade
) {
}
