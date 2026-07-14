package com.kiosk.domain.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 입고 수량에 따라 현재고와 재고 상태가 함께 바뀌는 경계값 규칙을 검증한다.
 * 특히 현재고가 안전재고와 '같을 때' LOW라는 정책을 명확히 남긴다.
 */
class BranchInventoryTest {

    @Test
    // 7개에 3개를 입고해 안전재고 10개와 같아지면 LOW를 유지해야 한다.
    void receiveKeepsLowStatusAtSafetyQuantity() {
        BranchInventory inventory = inventory(7, 10);

        inventory.receive(3);

        assertEquals(10, inventory.getCurrentQuantity());
        assertEquals(InventoryStatus.LOW, inventory.getInventoryStatus());
    }

    @Test
    // 안전재고보다 한 개라도 많아지면 정상 재고인 NORMAL로 바뀌어야 한다.
    void receiveChangesStatusToNormalAboveSafetyQuantity() {
        BranchInventory inventory = inventory(7, 10);

        inventory.receive(4);

        assertEquals(11, inventory.getCurrentQuantity());
        assertEquals(InventoryStatus.NORMAL, inventory.getInventoryStatus());
    }

    /** 현재고와 안전재고만 바꾸어 경계값 테스트에 재사용하는 객체 생성 헬퍼다. */
    private BranchInventory inventory(int currentQuantity, int safetyQuantity) {
        return BranchInventory.builder()
                .currentQuantity(currentQuantity)
                .safetyQuantity(safetyQuantity)
                .inventoryStatus(InventoryStatus.LOW)
                .build();
    }
}
