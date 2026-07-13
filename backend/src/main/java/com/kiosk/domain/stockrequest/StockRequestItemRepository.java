package com.kiosk.domain.stockrequest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRequestItemRepository extends JpaRepository<StockRequestItem, Long> {

    @Query("""
            SELECT i FROM StockRequestItem i
            JOIN FETCH i.flavor f
            WHERE i.stockRequest.stockRequestId IN :stockRequestIds
            """)
    List<StockRequestItem> findByStockRequestIdIn(@Param("stockRequestIds") List<Long> stockRequestIds);

    List<StockRequestItem> findByStockRequest_StockRequestId(Long stockRequestId);
}
