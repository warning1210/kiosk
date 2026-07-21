package com.kiosk.hq.delivery.dto;

import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 배송 관리 화면 한 줄에 필요한 정보만 담은 응답이다.
 *
 * <p>배송 화면은 신청 화면과 달리 품목을 하나하나 펼치기보다 "무슨 맛 몇 통을 어디로 보내는가"를
 * 요약해서 보여 주므로, 품목 목록 대신 {@code menuSummary}와 {@code totalTubs}로 압축한다.
 */
public record DeliveryResponse(
        Long stockRequestId,
        String requestNumber,
        String shipmentNumber,
        Long branchId,
        String branchName,
        String menuSummary,
        int totalTubs,
        StockRequestStatus requestStatus,
        Urgency urgency,
        String driverName,
        LocalDateTime requestedAt,
        LocalDateTime estimatedArrivalAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        boolean delayed
) {

    public static DeliveryResponse from(StockRequest request, List<StockRequestItem> items, LocalDateTime now) {
        return new DeliveryResponse(
                request.getStockRequestId(),
                request.getRequestNumber(),
                request.getShipmentNumber(),
                request.getBranch().getBranchId(),
                request.getBranch().getBranchName(),
                summarize(items),
                items.stream().mapToInt(DeliveryResponse::quantityOf).sum(),
                request.getRequestStatus(),
                request.getUrgency(),
                request.getDriverName(),
                request.getRequestedAt(),
                request.getEstimatedArrivalAt(),
                request.getShippedAt(),
                request.getDeliveredAt(),
                isDelayed(request, now)
        );
    }

    /** "31요거트 외 2종"처럼 첫 품목 + 나머지 개수로 줄인다. */
    private static String summarize(List<StockRequestItem> items) {
        if (items.isEmpty()) {
            return "-";
        }
        String first = items.get(0).getFlavor().getFlavorName();
        return items.size() == 1 ? first : first + " 외 " + (items.size() - 1) + "종";
    }

    /** 승인 수량이 정해졌으면 그 값을, 아니면 신청 수량을 센다. */
    private static int quantityOf(StockRequestItem item) {
        return item.getApprovedQuantity() != null ? item.getApprovedQuantity() : item.getRequestedQuantity();
    }

    /** 배송 중인데 도착 예정 시각이 이미 지났으면 지연으로 본다. */
    private static boolean isDelayed(StockRequest request, LocalDateTime now) {
        return request.getRequestStatus() == StockRequestStatus.SHIPPING
                && request.getEstimatedArrivalAt() != null
                && request.getEstimatedArrivalAt().isBefore(now);
    }
}
