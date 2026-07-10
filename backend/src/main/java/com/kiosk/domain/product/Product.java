package com.kiosk.domain.product;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.common.SaleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    @Column(name = "base_price", nullable = false)
    @Builder.Default
    private Integer basePrice = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "requires_flavor_selection", nullable = false)
    @Builder.Default
    private Boolean requiresFlavorSelection = false;

    @Column(name = "selectable_flavor_count", nullable = false, columnDefinition = "tinyint")
    @Builder.Default
    private Integer selectableFlavorCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "container_policy", nullable = false)
    @Builder.Default
    private ContainerPolicy containerPolicy = ContainerPolicy.NONE;

    @Column(name = "is_large", nullable = false)
    @Builder.Default
    private Boolean isLarge = false;

    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false)
    @Builder.Default
    private SaleStatus saleStatus = SaleStatus.ON_SALE;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
