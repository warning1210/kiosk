package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.util.List;

public record StockRequestResponse(
        Long stockRequestId,
        String requestNumber,
        Long branchId,
        String branchName,
        String requesterAdminName,
        StockRequestStatus requestStatus,
        Urgency urgency,
        String requestReason,
        String rejectionReason,
        LocalDateTime requestedAt,
        String processedAdminName,
        LocalDateTime processedAt,
        String trackingNumber,
        String courierName,
        String driverName,
        LocalDateTime estimatedArrivalAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<StockRequestItemResponse> items
) {

    public static StockRequestResponse from(StockRequest request, List<StockRequestItem> items) {
        return new StockRequestResponse(
                request.getStockRequestId(),
                request.getRequestNumber(),
                request.getBranch().getBranchId(),
                request.getBranch().getBranchName(),
                request.getRequesterAdmin().getName(),
                request.getRequestStatus(),
                request.getUrgency(),
                request.getRequestReason(),
                request.getRejectionReason(),
                request.getRequestedAt(),
                request.getProcessedAdmin() != null ? request.getProcessedAdmin().getName() : null,
                request.getProcessedAt(),
                request.getTrackingNumber(),
                request.getCourierName(),
                request.getDriverName(),
                request.getEstimatedArrivalAt(),
                request.getShippedAt(),
                request.getDeliveredAt(),
                items.stream().map(StockRequestItemResponse::from).toList()
        );
    }
}
