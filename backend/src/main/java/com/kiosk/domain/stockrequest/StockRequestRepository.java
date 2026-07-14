package com.kiosk.domain.stockrequest;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 재고 신청 헤더인 {@link StockRequest}를 저장하고 조회하는 Spring Data JPA 저장소이다.
 *
 * <p>일반 저장·PK 조회는 {@link JpaRepository}가 제공하고, 지점/본사 목록 조건과 동시 상태 변경에 필요한
 * 쿼리만 이 인터페이스에서 정의한다. 반환형이 {@link Page}인 조회는 전달받은 {@link Pageable}의 페이지
 * 크기와 위치를 적용하고 전체 건수도 함께 계산한다.
 */
public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {

    /** 특정 지점에서 상태가 일치하는 신청을 최신 신청순으로 조회한다. */
    Page<StockRequest> findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(
            Long branchId, StockRequestStatus status, Pageable pageable);

    /** 상태 필터 없이 특정 지점의 전체 신청을 최신 신청순으로 조회한다. */
    Page<StockRequest> findByBranch_BranchIdOrderByRequestedAtDesc(Long branchId, Pageable pageable);

    /**
     * 본사 화면의 선택 검색 조건을 한 쿼리로 처리한다.
     *
     * <p>각 조건이 null이면 해당 조건을 건너뛴다. keyword는 Service에서 소문자와 {@code %} 와일드카드를
     * 붙여 전달하므로 신청번호, 지점명 또는 신청 항목의 맛 이름 중 하나가 포함되면 검색된다. EXISTS를
     * 사용해 맛을 검색하더라도 StockRequest 결과 행이 항목 수만큼 중복되지 않게 한다.
     */
    @Query("""
            SELECT sr FROM StockRequest sr
            WHERE (:status IS NULL OR sr.requestStatus = :status)
              AND (:branchId IS NULL OR sr.branch.branchId = :branchId)
              AND (:fromDate IS NULL OR sr.requestedAt >= :fromDate)
              AND (:toDate IS NULL OR sr.requestedAt <= :toDate)
              AND (:keyword IS NULL
                   OR LOWER(sr.requestNumber) LIKE :keyword
                   OR LOWER(sr.branch.branchName) LIKE :keyword
                   OR EXISTS (
                       SELECT item.stockRequestItemId
                       FROM StockRequestItem item
                       WHERE item.stockRequest = sr
                         AND LOWER(item.flavor.flavorName) LIKE :keyword
                   ))
            ORDER BY sr.requestedAt DESC
            """)
    Page<StockRequest> searchForHq(
            @Param("status") StockRequestStatus status,
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** 본사 요약 영역에서 지정 상태의 신청 건수를 계산한다. */
    long countByRequestStatus(StockRequestStatus status);

    /** 본사 요약 영역에서 지정한 종료·대기 상태들을 제외한 진행 중 신청 건수를 계산한다. */
    long countByRequestStatusNotIn(Collection<StockRequestStatus> statuses);

    /**
     * 상태 전이(승인/반려/배송등록/수령확인/취소) 직전에 신청을 조회하면서 비관적 쓰기 락을 건다.
     *
     * <p>같은 PK의 신청을 두 요청이 동시에 바꾸려 할 때 먼저 락을 얻은 트랜잭션이 끝날 때까지 다른
     * 트랜잭션을 기다리게 해, 둘 다 이전 상태를 보고 처리하는 경쟁 상황을 막는다. 락은 반드시 Service의
     * 트랜잭션 안에서 호출해야 조회 뒤에도 유지되며, 실제 동작은 사용하는 DB의 락 지원을 전제로 한다.
     *
     * @param id 변경할 StockRequest PK
     * @return 신청이 없을 수도 있으므로 Optional로 감싼 조회 결과
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockRequest s WHERE s.stockRequestId = :id")
    Optional<StockRequest> findByIdForUpdate(@Param("id") Long id);
}
