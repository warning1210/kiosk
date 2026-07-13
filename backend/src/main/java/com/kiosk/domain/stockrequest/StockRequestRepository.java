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

public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {

    Page<StockRequest> findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(
            Long branchId, StockRequestStatus status, Pageable pageable);

    Page<StockRequest> findByBranch_BranchIdOrderByRequestedAtDesc(Long branchId, Pageable pageable);

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

    long countByRequestStatus(StockRequestStatus status);

    long countByRequestStatusNotIn(Collection<StockRequestStatus> statuses);

    /**
     * 상태 전이(승인/반려/배송등록/수령확인/취소) 처리 직전에 사용. 같은 신청 건에 대한 동시 처리를
     * 막기 위해 비관적 쓰기 락을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockRequest s WHERE s.stockRequestId = :id")
    Optional<StockRequest> findByIdForUpdate(@Param("id") Long id);
}
