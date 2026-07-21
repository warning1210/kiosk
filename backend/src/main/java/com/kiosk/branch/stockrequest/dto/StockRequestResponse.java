package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 신청 한 건을 화면에 보여 주기 위한 응답이다.
 *
 * <p>지점 화면과 본사 화면이 같은 신청 데이터를 보여 주므로 두 곳에서 함께 쓴다
 * (본사 쪽은 {@code com.kiosk.hq.stockrequest}에서 이 타입을 import 한다).
 * 엔티티를 그대로 내보내면 지연 로딩 프록시가 직렬화 시점에 터질 수 있어서, 필요한 값만 복사해 담는다.
 */
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
                // 신청·지점·신청자 식별 정보
                request.getStockRequestId(),
                request.getRequestNumber(),
                request.getBranch().getBranchId(),
                request.getBranch().getBranchName(),
                request.getRequesterAdmin().getName(),
                // 신청 내용과 현재 처리 상태
                request.getRequestStatus(),
                request.getUrgency(),
                request.getRequestReason(),
                request.getRejectionReason(),
                request.getRequestedAt(),
                // 본사의 승인·반려 처리 정보 (아직 처리 전이면 비어 있다)
                request.getProcessedAdmin() == null ? null : request.getProcessedAdmin().getName(),
                request.getProcessedAt(),
                // 배송이 시작된 뒤에 채워지는 운송 정보
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
