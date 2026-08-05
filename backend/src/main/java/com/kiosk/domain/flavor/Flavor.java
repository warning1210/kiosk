package com.kiosk.domain.flavor;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.common.SaleStatus;
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
public class Flavor {

    private Long flavorId;

    private Category category;

    private String flavorName;

    private String imageUrl;

    private String description;

    private String allergyInfo;

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
