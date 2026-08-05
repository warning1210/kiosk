package com.kiosk.domain.event;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.product.Product;
import java.math.BigDecimal;
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
public class Event {

    private Long eventId;

    private String eventName;

    private EventType eventType;

    // FLAVOR_DISCOUNT ?꾩슜 - 蹂몄젏??吏?먯뿉 ?섍만 ?좎씤 ?좏삎(??湲덉븸). MONTHLY_FLAVOR???좎씤???놁쑝誘濡?null.
    private BenefitType benefitType;

    private String imageUrl;

    private String description;

    private BigDecimal discountRate;

    private Integer discountAmount;

    // MONTHLY_FLAVOR ?꾩슜 - 蹂몄젏??吏곸젒 吏?뺥븳 ?좎씤 留?(??吏???먮룞 ?곸슜, 吏?먮퀎 ?좏깮 ?놁쓬)
    private Flavor flavor;

    private Product sizeUpFromProduct;

    private Product sizeUpToProduct;

    private Integer additionalPayment;

    private String targetJson;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    private Admin creatorAdmin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
