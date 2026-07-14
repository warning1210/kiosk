package com.kiosk.stockrequest.dto;

import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지점 API와 본사 API가 공통으로 반환하는 재고 신청의 읽기 전용 표현이다.
 *
 * <p>JPA 엔티티를 그대로 JSON으로 보내지 않고 응답 DTO로 복사하면, 지연 로딩 관계나
 * 내부 필드가 의도치 않게 노출되는 것을 막고 API 형식을 명시적으로 유지할 수 있다.
 * 상태 처리 단계에 따라 아직 정해지지 않은 값은 {@code null}일 수 있다.</p>
 *
 * @param stockRequestId DB에서 관계와 변경 대상을 식별하는 신청 기본키
 * @param requestNumber 사용자가 화면과 문서에서 확인하는 업무용 신청 번호
 * @param branchId 신청 지점 기본키
 * @param branchName 신청 지점 이름
 * @param requesterAdminName 최초 신청 관리자 이름
 * @param requestStatus 현재 신청 처리 상태
 * @param urgency 신청 긴급도
 * @param requestReason 신청 사유
 * @param rejectionReason 반려된 경우의 사유
 * @param requestedAt 최초 신청 시각
 * @param processedAdminName 본사에서 승인 또는 반려를 처리한 관리자 이름
 * @param processedAt 승인·반려 처리 시각
 * @param trackingNumber 배송 운송장 번호
 * @param courierName 택배사 이름
 * @param driverName 배송 기사 이름
 * @param estimatedArrivalAt 도착 예정 시각
 * @param shippedAt 배송 시작 시각
 * @param deliveredAt 지점 수령 확정 시각
 * @param items 맛별 신청 및 승인 수량 목록
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

    /**
     * 재고 신청 엔티티와 품목 엔티티를 하나의 API 응답으로 변환한다.
     *
     * <p>정적 팩토리 메서드에 변환 규칙을 모아두면 지점과 본사 서비스가 같은 필드 순서와
     * {@code null} 처리 규칙을 재사용할 수 있다.</p>
     *
     * @param request 응답의 신청 본문이 될 엔티티
     * @param items 응답에 포함할 신청 품목 엔티티 목록
     * @return 외부에 전달할 읽기 전용 재고 신청 응답
     */
    public static StockRequestResponse from(StockRequest request, List<StockRequestItem> items) {
        return new StockRequestResponse(
                // 신청, 지점, 신청자를 식별하는 기본 정보
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
                // 본사의 승인·반려 처리 정보
                processedAdminName(request),
                request.getProcessedAt(),
                // 배송이 시작된 뒤 채워지는 운송 정보
                request.getTrackingNumber(),
                request.getCourierName(),
                request.getDriverName(),
                request.getEstimatedArrivalAt(),
                request.getShippedAt(),
                request.getDeliveredAt(),
                // 품목 엔티티도 외부 응답 전용 DTO로 한 번 더 변환한다.
                items.stream().map(StockRequestItemResponse::from).toList()
        );
    }

    /**
     * 아직 아무도 처리하지 않은 신청에서도 안전하게 처리 관리자 이름을 구한다.
     *
     * @param request 처리 관리자 관계를 확인할 신청
     * @return 처리 전이면 {@code null}, 처리 후이면 관리자 이름
     */
    private static String processedAdminName(StockRequest request) {
        if (request.getProcessedAdmin() == null) {
            return null;
        }
        return request.getProcessedAdmin().getName();
    }
}
