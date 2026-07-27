package com.kiosk.kiosk.session.dto;

import java.util.List;

public record KioskDailySummaryResponse(
        int totalRevenue,
        int orderCount,
        List<PopularMenu> popularMenus
) {
    public record PopularMenu(String productName, int quantity) {
    }
}
