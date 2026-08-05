package com.kiosk.domain.inventory;

import com.kiosk.domain.branch.Branch;
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
public class BranchInventory {

    private Long branchInventoryId;

    private Branch branch;

    private Flavor flavor;

    @Builder.Default
    private Integer currentQuantity = 0;

    @Builder.Default
    private Integer safetyQuantity = 0;

    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.NORMAL;

    @Builder.Default
    private Boolean isKioskVisible = true;

    @Builder.Default
    private Boolean isBranchRecommended = false;

    @Builder.Default
    private Integer displayOrder = 0;

    private LocalDateTime updatedAt;

    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
