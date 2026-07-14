package com.kiosk.domain.inventory;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.stockrequest.StockRequest;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재고 수량이 변한 한 번의 사건을 기록하는 이력 엔티티이다.
 *
 * <p>{@link BranchInventory}가 현재 상태를 보여 주는 스냅샷이라면, 이 엔티티는 언제·왜·얼마나 변했고
 * 결과 수량이 얼마였는지를 시간순으로 남긴다. 재고 신청 수령 흐름에서는 현재 재고를 증가시킨 뒤 같은
 * 트랜잭션에서 {@link InventoryTransactionType#REQUEST_RECEIVED} 이력을 저장한다. 처리 중 런타임 예외가
 * 발생해 트랜잭션이 롤백되면 둘 다 되돌아가 현재 수량과 이력이 어긋나지 않는다.
 *
 * <p>여러 종류의 재고 변경이 공용으로 사용하는 테이블이므로 주문, 재고 신청, 처리 관리자 FK는 원인에
 * 따라 null일 수 있다. 서비스는 Builder로 해당 사건에 필요한 관계와 수량 스냅샷을 한 번에 구성한다.
 */
@Entity
@Table(name = "inventory_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    /** 재고 이력 한 건을 식별하는 DB 자동 생성 PK이다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private Long inventoryTransactionId;

    /** 변화가 발생한 지점. branch_id FK로 운영 주체를 바로 조회할 수 있게 한다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** 변경된 현재 재고 행. branch_inventory_id FK가 BranchInventory의 PK를 가리킨다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_inventory_id", nullable = false)
    private BranchInventory branchInventory;

    /** 어떤 맛의 수량이 변했는지 보존하는 flavor_id FK이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private Flavor flavor;

    /** 입고, 주문, 재고 신청 수령 등 수량 변경의 업무 원인이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private InventoryTransactionType transactionType;

    /** 이번 사건에서 증감한 수량. 재고 신청 수령 이력에는 입고한 양수가 들어간다. */
    @Column(name = "change_quantity", nullable = false)
    private Integer changeQuantity;

    /** 증감 처리가 끝난 직후의 재고 수량으로, 과거 시점의 결과를 재구성할 때 사용한다. */
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    /** 운영자가 이력을 이해할 수 있도록 남기는 설명이다. */
    @Column(name = "reason", length = 500)
    private String reason;

    /* 아래 FK들은 변경의 출처를 추적한다. 사건 종류에 맞는 관계만 채워지고 나머지는 null일 수 있다. */

    /** 주문 때문에 발생한 이력일 때 원본 Order를 가리킨다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** 재고 신청 수령 때문에 발생한 이력일 때 원본 StockRequest를 가리킨다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_request_id")
    private StockRequest stockRequest;

    /** 수동 처리 또는 수령 확인을 수행한 관리자이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_admin_id")
    private Admin processedAdmin;

    /** 재고 사건이 발생한 업무 시각. 여러 항목의 수령 시각을 맞추기 위해 Service가 직접 전달한다. */
    @Column(name = "transaction_at", nullable = false)
    private LocalDateTime transactionAt;
}
