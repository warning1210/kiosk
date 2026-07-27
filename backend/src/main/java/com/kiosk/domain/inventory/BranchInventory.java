package com.kiosk.domain.inventory;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
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
@Table(name = "branch_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_inventory_id")
    private Long branchInventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private Flavor flavor;

    @Column(name = "current_quantity", nullable = false)
    @Builder.Default
    private Integer currentQuantity = 0;

    @Column(name = "safety_quantity", nullable = false)
    @Builder.Default
    private Integer safetyQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false)
    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.NORMAL;

    @Column(name = "is_kiosk_visible", nullable = false)
    @Builder.Default
    private Boolean isKioskVisible = true;

    @Column(name = "is_branch_recommended", nullable = false)
    @Builder.Default
    private Boolean isBranchRecommended = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
