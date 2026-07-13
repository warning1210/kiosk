package com.kiosk.domain.stockrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

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
}
