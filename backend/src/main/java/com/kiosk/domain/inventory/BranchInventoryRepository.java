package com.kiosk.domain.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link BranchInventory}의 저장과 조회를 담당하는 Spring Data JPA 저장소이다.
 *
 * <p>PK 기반의 기본 CRUD는 {@link JpaRepository}가 제공하고, 업무에서 자주 사용하는 지점+맛 조합 조회와
 * 재고 갱신용 잠금 조회, 지점 재고 검색을 추가로 선언한다.
 */
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {

    /**
     * 지점 PK와 맛 PK의 조합으로 현재 재고 한 행을 조회한다.
     * 테이블의 복합 UNIQUE 제약 덕분에 결과가 최대 하나이므로 Optional을 반환한다.
     */
    Optional<BranchInventory> findByBranch_BranchIdAndFlavor_FlavorId(Long branchId, Long flavorId);

    /**
     * 입고처럼 수량을 변경하기 직전에 지점+맛 재고를 비관적 쓰기 잠금과 함께 조회한다.
     *
     * <p>동시에 두 요청이 같은 현재 수량을 읽고 각각 덮어쓰는 갱신 유실을 막기 위해, 먼저 조회한
     * 트랜잭션이 끝날 때까지 다음 트랜잭션을 기다리게 한다. 잠금이 조회 이후에도 유지되려면 반드시
     * Service의 트랜잭션 안에서 호출해야 하며 실제 동작은 사용하는 DB의 락 지원을 전제로 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT bi FROM BranchInventory bi
            WHERE bi.branch.branchId = :branchId
              AND bi.flavor.flavorId = :flavorId
            """)
    Optional<BranchInventory> findByBranchAndFlavorForUpdate(
            @Param("branchId") Long branchId,
            @Param("flavorId") Long flavorId);

    /**
     * 지점 재고 화면에서 카테고리와 맛 이름으로 재고를 검색한다.
     *
     * <p>Flavor는 화면에 반드시 필요하므로 JOIN FETCH로 함께 읽고, 카테고리가 없는 맛도 검색 대상이 될 수
     * 있도록 LEFT JOIN을 사용한다. 조건값이 null이면 해당 필터를 건너뛴다. 결과는 안전 재고와의 차이가
     * 작은 순서로 정렬되어 부족 위험이 큰 상품을 먼저 보여 준다.
     */
    @Query("""
            SELECT bi FROM BranchInventory bi
            JOIN FETCH bi.flavor f
            LEFT JOIN f.category c
            WHERE bi.branch.branchId = :branchId
              AND (:categoryId IS NULL OR c.categoryId = :categoryId)
              AND (:keyword IS NULL OR f.flavorName LIKE CONCAT('%', :keyword, '%'))
            ORDER BY (bi.currentQuantity - bi.safetyQuantity) ASC, f.flavorName ASC
            """)
    List<BranchInventory> search(@Param("branchId") Long branchId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword);
}
