package com.kiosk.branch.inventory.dto;

// type: "IN"(입고) / "SET"(실사 보정, 절대값 지정) / 그 외는 "OUT"(차감)으로 취급
public record InventoryAdjustRequest(String type, Integer grams) {
}
