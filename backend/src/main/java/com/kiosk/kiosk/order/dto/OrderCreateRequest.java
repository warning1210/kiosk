package com.kiosk.kiosk.order.dto;

import com.kiosk.domain.order.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    private Long branchId;
    private OrderType orderType;
    private Integer usedPoints;
    
    // 프론트엔드(Vue)에서 쿠키로 관리하던 장바구니 리스트를 결제 시점에 한 번에 보냅니다.
    private List<OrderItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private String productNameSnapshot;
        private Integer unitPriceSnapshot;
        private Integer quantity;
        private List<FlavorRequest> flavors;
    }

    @Getter
    @NoArgsConstructor
    public static class FlavorRequest {
        private Long flavorId;
        private String flavorNameSnapshot;
    }
}
