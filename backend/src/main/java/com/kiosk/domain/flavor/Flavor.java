package com.kiosk.domain.flavor;

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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flavor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flavor_id")
    private Long flavorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "flavor_name", length = 100, nullable = false)
    private String flavorName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "allergy_info", length = 500)
    private String allergyInfo;

    @Column(name = "source_seq")
    private Integer sourceSeq;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

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
