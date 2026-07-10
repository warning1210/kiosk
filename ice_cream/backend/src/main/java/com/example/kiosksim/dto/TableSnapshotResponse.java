package com.example.kiosksim.dto;

import java.util.List;

public record TableSnapshotResponse(
        List<OrderResponse> orders,
        long orderCount,
        long orderItemCount,
        long orderItemFlavorCount,
        long paymentCount
) {
}
