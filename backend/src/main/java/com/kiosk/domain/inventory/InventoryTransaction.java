package com.kiosk.domain.inventory;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.stockrequest.StockRequest;
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
public class InventoryTransaction {

    private Long inventoryTransactionId;

    private Branch branch;

    private BranchInventory branchInventory;

    private Flavor flavor;

    private InventoryTransactionType transactionType;

    private Integer changeQuantity;

    private Integer quantityAfter;

    private String reason;

    private Order order;

    private StockRequest stockRequest;

    private Admin processedAdmin;

    private LocalDateTime transactionAt;
}
