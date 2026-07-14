package com.kiosk.domain.inventory;

/**
 * 현재 수량과 안전 재고를 비교한 지점 재고 상태이다.
 *
 * <p>{@link BranchInventory#receive(int)}가 입고 후 상태를 다시 계산한다. 이 값은 화면 표시와 품절 판단을
 * 빠르게 하기 위해 수량과 함께 저장되는 값이므로, 수량 변경 시 함께 갱신해야 한다.
 */
public enum InventoryStatus {
    /** 현재 수량이 안전 재고보다 많아 판매 여유가 있다. */
    NORMAL,

    /** 재고가 남아 있지만 현재 수량이 안전 재고 이하이다. */
    LOW,

    /** 현재 수량이 0 이하라 판매할 수 없다. */
    SOLD_OUT
}
