package com.kiosk.domain.order;

import com.kiosk.domain.product.Product;
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
public class OrderItem {

    private Long orderItemId;

    private Order order;

    private Product product;

    private String productNameSnapshot;

    private Integer unitPriceSnapshot;

    private Integer quantity;

    private Integer itemTotal;

    @Builder.Default
    private ContainerType containerType = ContainerType.NONE;

    @Builder.Default
    private Byte spoonCount = 0;

    private Byte dryIceMinutes;

    private String requestNote;

    private LocalDateTime createdAt;

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
