package com.kiosk.domain.inventory;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 지점이 특정 맛을 몇 개 보유하는지 나타내는 현재 재고 엔티티이다.
 *
 * <p>테이블의 PK는 {@code branchInventoryId}지만, 업무상으로는 지점과 맛의 조합 하나당 재고 행도 하나만
 * 있어야 한다. {@link UniqueConstraint}가 {@code branch_id + flavor_id} 중복을 DB 수준에서도 막는다.
 *
 * <p>JPA 조회용 기본 생성자와 서비스/테스트에서 값을 명시하기 위한 Builder를 제공한다. 조회한 엔티티를
 * Service 트랜잭션 안에서 {@link #receive(int)}로 바꾸면 별도 save 호출 없이 dirty checking으로 수량과
 * 상태가 함께 UPDATE된다. 동시 수령으로 갱신 값이 유실되지 않도록 실제 입고 흐름은 Repository의 비관적
 * 잠금 조회를 사용한다.
 */
@Entity
@Table(
        name = "branch_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_branch_inventory_branch_flavor",
                columnNames = {"branch_id", "flavor_id"}
        )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInventory {

    /** 재고 행 자체의 PK로, 재고 변경 이력이 이 값을 FK로 참조한다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_inventory_id")
    private Long branchInventoryId;

    /** 재고를 보유한 지점. branch_id FK이며 한 지점에는 여러 맛의 재고 행이 존재한다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** 재고 대상 상품(맛). flavor_id FK이며 같은 맛도 지점별로 서로 다른 재고를 가진다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private Flavor flavor;

    /** 현재 판매 가능한 수량이다. 새 재고 행은 기본 0개로 시작한다. */
    @Column(name = "current_quantity", nullable = false)
    @Builder.Default
    private Integer currentQuantity = 0;

    /** LOW 상태를 판단하는 지점 운영 기준 수량이다. */
    @Column(name = "safety_quantity", nullable = false)
    @Builder.Default
    private Integer safetyQuantity = 0;

    /** 현재 수량과 안전 수량을 비교한 상태로, 수량과 함께 갱신해야 하는 파생 값이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false)
    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.NORMAL;

    /* 아래 값들은 키오스크 노출과 지점별 진열 정책을 위한 설정이며 실제 보유 수량과는 별개이다. */

    @Column(name = "is_kiosk_visible", nullable = false)
    @Builder.Default
    private Boolean isKioskVisible = true;

    @Column(name = "is_branch_recommended", nullable = false)
    @Builder.Default
    private Boolean isBranchRecommended = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** 재고 행이 마지막으로 INSERT 또는 UPDATE된 시각이다. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 수령한 수량을 현재 수량에 더하고, 변경된 수량을 기준으로 재고 상태도 즉시 다시 계산한다.
     *
     * <p>이 메서드는 입고라는 한 업무 동작에서 반드시 함께 바뀌어야 하는 두 필드를 묶는다. 정상 생성
     * 흐름에서는 요청 DTO의 수량 검증과 그 수량을 복사한 승인 값에 의존하며, 이 메서드 자체는 양수 여부를
     * 다시 검사하지 않으므로 검증되지 않은 값을 직접 넘기면 안 된다.
     */
    public void receive(int quantity) {
        this.currentQuantity += quantity;
        this.inventoryStatus = calculateStatus();
    }

    /** 0 이하이면 품절, 안전 재고 이하이면 부족, 그보다 많으면 정상이라는 경계 규칙을 한곳에 모은다. */
    private InventoryStatus calculateStatus() {
        if (currentQuantity <= 0) {
            return InventoryStatus.SOLD_OUT;
        }
        if (currentQuantity <= safetyQuantity) {
            return InventoryStatus.LOW;
        }
        return InventoryStatus.NORMAL;
    }

    /** JPA가 INSERT/UPDATE하기 직전에 마지막 수정 시각을 자동으로 갱신하는 생명주기 콜백이다. */
    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
