package com.kiosk.domain.product;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.common.SaleStatus;
import java.time.LocalDate;
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
public class Product {

    private Long productId;

    private Category category;

    private String productName;

    @Builder.Default
    private Integer basePrice = 0;

    private String imageUrl;

    private String description;

    @Builder.Default
    private Boolean requiresFlavorSelection = false;

    @Builder.Default
    private Byte selectableFlavorCount = 0;

    @Builder.Default
    private ContainerPolicy containerPolicy = ContainerPolicy.NONE;

    @Builder.Default
    private Boolean isLarge = false;

    @Builder.Default
    private Boolean isNew = false;

    private LocalDate releaseDate;

    @Builder.Default
    private SaleStatus saleStatus = SaleStatus.ON_SALE;

    @Builder.Default
    private Boolean isVisible = true;

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
