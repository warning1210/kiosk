package com.kiosk.domain.stockrequest;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_request_id")
    private Long stockRequestId;

    @Column(name = "request_number", length = 30, unique = true, nullable = false)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_admin_id", nullable = false)
    private Admin requesterAdmin;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private RequestType requestType = RequestType.RESTOCK;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    @Builder.Default
    private StockRequestStatus requestStatus = StockRequestStatus.PENDING;

    @Column(name = "request_reason", length = 500)
    private String requestReason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    @Builder.Default
    private Urgency urgency = Urgency.NORMAL;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_admin_id")
    private Admin processedAdmin;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "shipment_number", length = 30)
    private String shipmentNumber;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "driver_name", length = 50)
    private String driverName;

    @Column(name = "estimated_arrival_at")
    private LocalDateTime estimatedArrivalAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_confirmed_admin_id")
    private Admin receiptConfirmedAdmin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 아래는 신청 처리 흐름(신청 -> 승인/반려 -> 배송 -> 수령확인)에서 한 단계가 넘어갈 때
    // 함께 바뀌어야 하는 필드들을 하나로 묶은 메서드다. setter를 여러 번 호출하는 대신 이 메서드를 쓰면
    // "어느 단계로 넘어가는 중인지"가 호출부에서 바로 읽힌다.
    // 지금 상태에서 그 단계로 넘어가도 되는지에 대한 검사는 Service가 담당한다.

    /** PK가 만들어진 뒤에 REQ-날짜-PK 형태의 업무용 신청번호를 확정한다. */
    public void assignRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    /** 지점이 아직 처리되지 않은 신청을 스스로 취소한다. */
    public void cancel() {
        this.requestStatus = StockRequestStatus.CLOSED;
    }

    /** 본사가 승인하면 곧바로 출고 준비(PREPARING) 단계로 넘어간다. */
    public void approve(Admin admin, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.PREPARING;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /** 본사가 사유와 함께 반려한다. */
    public void reject(Admin admin, String rejectionReason, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /** 본사가 운송 정보를 등록하고 배송중(SHIPPING) 상태로 전환한다. */
    public void startShipping(
            String trackingNumber,
            String courierName,
            String driverName,
            LocalDateTime estimatedArrivalAt,
            LocalDateTime shippedAt) {
        this.requestStatus = StockRequestStatus.SHIPPING;
        this.trackingNumber = trackingNumber;
        this.courierName = courierName;
        this.driverName = driverName;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.shippedAt = shippedAt;
    }

    /** 지점이 실물 수령을 확인한다. 실제 재고 반영은 지점 재고 화면의 입고 처리에서 이뤄진다. */
    public void confirmReceipt(Admin admin, LocalDateTime deliveredAt) {
        this.requestStatus = StockRequestStatus.DELIVERED;
        this.receiptConfirmedAdmin = admin;
        this.deliveredAt = deliveredAt;
    }
}
