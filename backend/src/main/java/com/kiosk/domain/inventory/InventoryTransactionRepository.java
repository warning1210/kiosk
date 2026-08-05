package com.kiosk.domain.inventory;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryTransactionRepository {
    int insert(InventoryTransaction transaction);
    default InventoryTransaction save(InventoryTransaction transaction) { insert(transaction); return transaction; }
}
