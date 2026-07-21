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

    // --- 지점 화면: 내 지점 신청 목록 (상태 필터 유무에 따라 두 가지) ---

    Page<StockRequest> findByBranch_BranchIdOrderByRequestedAtDesc(Long branchId, Pageable pageable);

    Page<StockRequest> findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(
            Long branchId, StockRequestStatus status, Pageable pageable);

    // --- 본사 화면: 전 지점 신청 검색 ---

    // 안 보낸 조건은 null로 들어와서 그 줄이 통째로 참이 되므로, 실제로 보낸 조건만 걸러진다.
    // keyword는 호출부에서 소문자 + %...% 형태로 만들어 넘긴다.
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

    // 승인/반려/배송/수령확인은 같은 신청 건을 두 사람이 동시에 건드릴 수 있어서,
    // 상태를 바꾸기 전에 이 메서드로 행을 잠그고 시작한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockRequest s WHERE s.stockRequestId = :id")
    Optional<StockRequest> findByIdForUpdate(@Param("id") Long id);
}
