package com.kiosk.domain.order;

import com.kiosk.domain.flavor.Flavor;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemFlavor {

    private Long orderItemFlavorId;

    private OrderItem orderItem;

    private Flavor flavor;

    private String flavorNameSnapshot;

    private Byte selectionOrder;

    @Builder.Default
    private Byte quantity = 1;

    private LocalDateTime createdAt;

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
