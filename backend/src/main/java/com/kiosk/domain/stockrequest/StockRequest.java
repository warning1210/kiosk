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

/**
 * 지점이 본사에 보낸 재고 신청 한 건의 공통 정보(헤더)를 표현하는 JPA 엔티티이다.
 *
 * <p>신청에 포함된 맛별 수량은 반복되는 값이므로 이 테이블에 넣지 않고 {@link StockRequestItem} 행으로
 * 분리한다. 이 엔티티의 PK인 {@code stockRequestId}는 테이블 관계를 맺는 기술 식별자이고,
 * {@code requestNumber}는 사용자 화면과 업무 기록에 보여 주는 업무 식별자이다.
 *
 * <p>{@link NoArgsConstructor}가 만드는 기본 생성자는 JPA가 DB 조회 결과로 객체를 복원할 때 필요하다.
 * 서비스와 테스트에서는 {@link Builder}를 통해 생성에 필요한 값의 이름을 확인하면서 객체를 만든다.
 * 모든 필드의 setter를 열어 두지 않고 상태 전환 메서드만 제공하여, 여러 필드가 함께 바뀌어야 하는
 * 업무 동작을 한곳에서 읽을 수 있게 했다.
 *
 * <p>조회한 엔티티가 트랜잭션 안에서 영속 상태라면 아래 메서드로 바뀐 값은 JPA의 dirty checking(변경
 * 감지)에 의해 커밋 시 UPDATE된다. 단, 허용되는 이전 상태인지에 대한 검사는 현재 Service의 책임이다.
 */
@Entity
@Table(name = "stock_request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequest {

    /** DB가 자동 생성하는 PK. 다른 테이블은 이 값으로 신청을 참조한다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_request_id")
    private Long stockRequestId;

    /** 화면·검색·이력 문구에 사용하는 업무 번호. PK가 생성된 뒤 {@code REQ-날짜-PK} 형태로 확정한다. */
    @Column(name = "request_number", length = 30, unique = true, nullable = false)
    private String requestNumber;

    /*
     * 아래 두 연관관계는 FK 값을 Long으로만 들고 있지 않고 실제 엔티티로 연결한다.
     * LAZY이므로 신청만 조회할 때 지점/관리자 전체를 즉시 읽지 않는다. 프록시가 아직 초기화되지 않았다면
     * 관련 필드 접근 시 조회할 수 있도록 트랜잭션 등 영속성 컨텍스트가 열려 있어야 한다.
     */

    /** 이 신청을 올린 지점. stock_request.branch_id FK가 Branch의 PK를 가리킨다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** 신청을 등록한 관리자. 여러 신청을 한 관리자가 만들 수 있으므로 ManyToOne 관계이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_admin_id", nullable = false)
    private Admin requesterAdmin;

    /* 신청 성격과 현재 처리 상태. enum 이름을 문자열로 저장하여 DB에서도 의미를 바로 확인할 수 있다. */

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private RequestType requestType = RequestType.RESTOCK;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    @Builder.Default
    private StockRequestStatus requestStatus = StockRequestStatus.PENDING;

    /** 지점이 신청이 필요한 이유를 자유롭게 기록한 내용이다. */
    @Column(name = "request_reason", length = 500)
    private String requestReason;

    /** 본사가 반려할 때 남긴 사유이며, 반려 전에는 null이다. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    @Builder.Default
    private Urgency urgency = Urgency.NORMAL;

    /** 지점이 실제로 신청을 제출한 업무 시각이다. */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /* 본사의 승인·반려 처리 정보. 아직 처리되지 않은 신청에서는 두 값 모두 null일 수 있다. */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_admin_id")
    private Admin processedAdmin;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /* 배송 단계에서 채워지는 정보. 신청 직후에는 값이 없으므로 nullable 필드로 둔다. */

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

    /** 지점에서 실제 수령을 확인한 관리자. 배송 완료 전에는 null이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_confirmed_admin_id")
    private Admin receiptConfirmedAdmin;

    /* 생성/수정 시각은 업무 시각과 별개인 DB 행 감사 정보이며 JPA 생명주기 콜백이 관리한다. */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * DB PK가 발급된 뒤 임시 번호를 최종 업무 번호로 교체한다.
     *
     * <p>IDENTITY 방식의 PK는 먼저 INSERT해야 알 수 있으므로 생성 Service는 임시 고유 번호로 저장한 뒤
     * 이 메서드를 호출한다. 같은 트랜잭션 안의 영속 엔티티이므로 다시 save하지 않아도 변경 감지가
     * 최종 번호를 UPDATE한다.
     */
    public void assignRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    /**
     * 지점이 대기 중인 신청을 취소하여 종료 상태로 만든다.
     * 호출 가능한 상태인지와 요청한 관리자가 같은 지점 소속인지는 Service에서 먼저 검사한다.
     */
    public void cancel() {
        this.requestStatus = StockRequestStatus.CLOSED;
    }

    /**
     * 본사 승인을 기록하고 배송 준비 단계로 이동한다.
     * 상태뿐 아니라 누가 언제 처리했는지도 함께 바꾸어 신청 이력이 서로 어긋나지 않게 한다.
     */
    public void approve(Admin admin, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.PREPARING;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /**
     * 본사 반려 결과와 사유, 처리자를 한 번에 기록한다.
     * 호출 전에 PENDING 상태인지 확인하는 책임은 Service에 있다.
     */
    public void reject(Admin admin, String rejectionReason, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /**
     * 준비 중인 신청에 운송 정보를 기록하고 배송 중 상태로 전환한다.
     * 서로 관련된 운송 필드를 한 메서드에서 변경하여 일부 정보만 저장되는 실수를 줄인다.
     */
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

    /**
     * 지점의 수령 확인 담당자와 완료 시각을 기록하고 배송 완료 상태로 전환한다.
     * 실제 수량 증가와 재고 거래 이력 저장은 같은 트랜잭션의 Service에서 함께 처리한다.
     */
    public void confirmReceipt(Admin admin, LocalDateTime deliveredAt) {
        this.requestStatus = StockRequestStatus.DELIVERED;
        this.receiptConfirmedAdmin = admin;
        this.deliveredAt = deliveredAt;
    }

    /** 최초 INSERT 직전에 생성·수정 시각을 같은 값으로 초기화하는 JPA 생명주기 콜백이다. */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 기존 행이 UPDATE되기 직전에 마지막 수정 시각을 갱신하는 JPA 생명주기 콜백이다. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
