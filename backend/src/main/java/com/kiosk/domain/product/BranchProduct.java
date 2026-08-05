package com.kiosk.domain.product;

import com.kiosk.domain.branch.Branch;
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
public class BranchProduct {

    private Long branchProductId;

    private Branch branch;

    private Product product;

    @Builder.Default
    private Boolean isVisible = true;

    @Builder.Default
    private SaleStatus saleStatus = SaleStatus.ON_SALE;

    @Builder.Default
    private Integer displayOrder = 0;

    private LocalDateTime updatedAt;

    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
