package com.kiosk.domain.stockrequest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRequestItemRepository extends JpaRepository<StockRequestItem, Long> {

    @Query("""
            select sri from StockRequestItem sri
            where sri.stockRequest.branch.branchId = :branchId
              and sri.flavor.flavorId = :flavorId
              and sri.stockRequest.requestStatus in :statuses
            order by sri.stockRequest.requestedAt desc
            """)
    List<StockRequestItem> findActive(@Param("branchId") Long branchId,
                                       @Param("flavorId") Long flavorId,
                                       @Param("statuses") List<StockRequestStatus> statuses);

    // 신청 목록 화면에서 신청마다 품목 쿼리가 따로 나가는 N+1을 피하려고,
    // 현재 페이지에 실린 신청 ID를 한 번에 넘겨 품목을 모아 온다.
    // 품목마다 맛 이름을 바로 쓰기 때문에 flavor는 JOIN FETCH로 같이 읽는다.
    @Query("""
            SELECT i FROM StockRequestItem i
            JOIN FETCH i.flavor f
            WHERE i.stockRequest.stockRequestId IN :stockRequestIds
            """)
    List<StockRequestItem> findByStockRequestIdIn(@Param("stockRequestIds") List<Long> stockRequestIds);

    List<StockRequestItem> findByStockRequest_StockRequestId(Long stockRequestId);
}
