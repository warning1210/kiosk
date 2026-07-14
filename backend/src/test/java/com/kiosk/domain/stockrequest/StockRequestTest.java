package com.kiosk.domain.stockrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.kiosk.domain.admin.Admin;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * {@link StockRequest}의 상태 변경 메서드가 서로 관련된 필드를 빠짐없이 바꾸는지 확인한다.
 * 외부 의존성이 없는 순수 도메인 테스트이므로 객체를 만들고 메서드를 직접 호출한다.
 */
class StockRequestTest {

    @Test
    // 승인 한 번으로 상태, 처리자, 처리 시간이 한 묶음으로 변경되는지 검증한다.
    void approveChangesStatusAndRecordsProcessor() {
        // 준비(Given)
        Admin admin = Admin.builder().adminId(1L).build();
        LocalDateTime processedAt = LocalDateTime.of(2026, 7, 14, 10, 0);
        StockRequest request = requestWithStatus(StockRequestStatus.PENDING);

        // 실행(When)
        request.approve(admin, processedAt);

        // 검증(Then)
        assertEquals(StockRequestStatus.PREPARING, request.getRequestStatus());
        assertSame(admin, request.getProcessedAdmin());
        assertEquals(processedAt, request.getProcessedAt());
    }

    @Test
    // 반려 상태와 함께 나중에 확인할 반려 사유가 남는지 검증한다.
    void rejectChangesStatusAndRecordsReason() {
        Admin admin = Admin.builder().adminId(1L).build();
        LocalDateTime processedAt = LocalDateTime.of(2026, 7, 14, 10, 0);
        StockRequest request = requestWithStatus(StockRequestStatus.PENDING);

        request.reject(admin, "본점 재고 부족", processedAt);

        assertEquals(StockRequestStatus.REJECTED, request.getRequestStatus());
        assertEquals("본점 재고 부족", request.getRejectionReason());
        assertSame(admin, request.getProcessedAdmin());
        assertEquals(processedAt, request.getProcessedAt());
    }

    @Test
    // 배송 시작 시 화면과 입고 처리에 필요한 배송 정보가 모두 기록되는지 검증한다.
    void startShippingRecordsDeliveryInformation() {
        LocalDateTime estimatedArrivalAt = LocalDateTime.of(2026, 7, 15, 14, 0);
        LocalDateTime shippedAt = LocalDateTime.of(2026, 7, 14, 11, 0);
        StockRequest request = requestWithStatus(StockRequestStatus.PREPARING);

        request.startShipping("TRACK-100", "기본택배", "홍길동", estimatedArrivalAt, shippedAt);

        assertEquals(StockRequestStatus.SHIPPING, request.getRequestStatus());
        assertEquals("TRACK-100", request.getTrackingNumber());
        assertEquals("기본택배", request.getCourierName());
        assertEquals("홍길동", request.getDriverName());
        assertEquals(estimatedArrivalAt, request.getEstimatedArrivalAt());
        assertEquals(shippedAt, request.getShippedAt());
    }

    @Test
    // 지점의 입고 확인이 최종 상태와 실제 확인자를 함께 기록하는지 검증한다.
    void confirmReceiptChangesStatusAndRecordsReceiver() {
        Admin admin = Admin.builder().adminId(1L).build();
        LocalDateTime deliveredAt = LocalDateTime.of(2026, 7, 15, 13, 0);
        StockRequest request = requestWithStatus(StockRequestStatus.SHIPPING);

        request.confirmReceipt(admin, deliveredAt);

        assertEquals(StockRequestStatus.DELIVERED, request.getRequestStatus());
        assertSame(admin, request.getReceiptConfirmedAdmin());
        assertEquals(deliveredAt, request.getDeliveredAt());
    }

    /** 각 테스트가 관심 있는 시작 상태만 지정해 신청 객체를 만드는 헬퍼다. */
    private StockRequest requestWithStatus(StockRequestStatus status) {
        return StockRequest.builder()
                .requestStatus(status)
                .build();
    }
}
