package com.kiosk.domain.stockrequest;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockRequestItemRepository {

    List<StockRequestItem> findActive(@Param("branchId") Long branchId,
                                       @Param("flavorId") Long flavorId,
                                       @Param("statuses") List<StockRequestStatus> statuses);

    // ?좎껌 紐⑸줉 ?붾㈃?먯꽌 ?좎껌留덈떎 ?덈ぉ 荑쇰━媛 ?곕줈 ?섍???N+1???쇳븯?ㅺ퀬,
    // ?꾩옱 ?섏씠吏???ㅻ┛ ?좎껌 ID瑜???踰덉뿉 ?섍꺼 ?덈ぉ??紐⑥븘 ?⑤떎.
    // ?덈ぉ留덈떎 留??대쫫??諛붾줈 ?곌린 ?뚮Ц??flavor??JOIN FETCH濡?媛숈씠 ?쎈뒗??
    List<StockRequestItem> findByStockRequestIdIn(@Param("stockRequestIds") List<Long> stockRequestIds);

    List<StockRequestItem> findByStockRequest_StockRequestId(Long stockRequestId);
    int insert(StockRequestItem item);
    int update(StockRequestItem item);
    default StockRequestItem save(StockRequestItem item) { if (item.getStockRequestItemId() == null) insert(item); else update(item); return item; }
    default List<StockRequestItem> saveAll(List<StockRequestItem> values) { values.forEach(this::save); return values; }
}
