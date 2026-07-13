package com.kiosk.domain.stockrequest;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRequestRepository extends JpaRepository<StockRequest, Long>, JpaSpecificationExecutor<StockRequest> {

    Page<StockRequest> findByBranch_BranchIdAndRequestStatus(Long branchId, StockRequestStatus status, Pageable pageable);

    Page<StockRequest> findByBranch_BranchId(Long branchId, Pageable pageable);

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
