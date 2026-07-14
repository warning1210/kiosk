package com.kiosk.domain.stockrequest;

import com.kiosk.domain.flavor.Flavor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 신청 한 건에 포함된 맛 하나와 신청 수량을 나타내는 상세 행이다.
 *
 * <p>{@link StockRequest}에는 신청자·상태 같은 공통 정보를 한 번만 저장하고, 맛마다 달라지는 수량은 이
 * 엔티티의 여러 행으로 저장한다. 예를 들어 신청 하나에 맛 세 개가 있다면 StockRequest 한 행과
 * StockRequestItem 세 행이 만들어진다.
 *
 * <p>JPA가 조회 결과를 복원할 수 있도록 기본 생성자를 두고, 서비스에서는 Builder로 관계와 수량을
 * 명시하여 생성한다. setter 대신 승인 수량을 정하는 업무 메서드를 제공하며, 영속 상태에서 변경하면
 * 트랜잭션 커밋 시 dirty checking으로 UPDATE된다.
 */
@Entity
@Table(name = "stock_request_item")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequestItem {

    /** 상세 행 자체의 PK. 여러 맛 항목을 각각 구분하기 위해 신청 PK와 별도로 둔다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_request_item_id")
    private Long stockRequestItemId;

    /**
     * 이 항목이 속한 신청 헤더. stock_request_id FK가 StockRequest의 PK를 가리킨다.
     * 여러 항목이 하나의 신청에 속하므로 ManyToOne 관계이며 필요할 때만 읽도록 LAZY로 설정했다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_request_id", nullable = false)
    private StockRequest stockRequest;

    /** 신청한 상품(맛). flavor_id FK로 같은 Flavor를 참조하는 여러 신청 항목이 존재할 수 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private Flavor flavor;

    /** 지점이 처음 요청한 수량으로, 신청 원본이므로 승인 이후에도 그대로 보존한다. */
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    /** 본사가 실제 승인한 수량. 승인 전에는 null이며 현재 승인 기능은 요청 수량 전체를 승인한다. */
    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    /** 상세 행이 최초 저장된 시각. 수정 시각이 아니라 생성 이력만 기록한다. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 현재 기능의 전체 승인 규칙에 따라 승인 수량을 요청 수량과 같게 기록한다. */
    public void approveRequestedQuantity() {
        this.approvedQuantity = this.requestedQuantity;
    }

    /**
     * 수령 확인 시 지점 재고에 더할 최종 수량을 반환한다.
     * 승인 수량이 기록되어 있으면 그 값을 우선하고, 이전 데이터처럼 값이 없으면 요청 수량을 사용한다.
     */
    public int getQuantityToReceive() {
        if (approvedQuantity != null) {
            return approvedQuantity;
        }
        return requestedQuantity;
    }

    /** JPA가 INSERT하기 직전에 생성 시각을 채우는 생명주기 콜백이다. */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
