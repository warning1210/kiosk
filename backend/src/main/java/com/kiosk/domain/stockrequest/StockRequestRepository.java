package com.kiosk.domain.stockrequest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.PageImpl;
import java.util.List;

@Mapper
public interface StockRequestRepository {

    // --- 吏???붾㈃: ??吏???좎껌 紐⑸줉 (?곹깭 ?꾪꽣 ?좊Т???곕씪 ??媛吏) ---

    default Page<StockRequest> findByBranch_BranchIdOrderByRequestedAtDesc(Long branchId, Pageable pageable) { return new PageImpl<>(selectBranch(branchId, null, pageable.getOffset(), pageable.getPageSize()), pageable, countBranch(branchId, null)); }

    default Page<StockRequest> findByBranch_BranchIdAndRequestStatusOrderByRequestedAtDesc(Long branchId, StockRequestStatus status, Pageable pageable) { return new PageImpl<>(selectBranch(branchId, status, pageable.getOffset(), pageable.getPageSize()), pageable, countBranch(branchId, status)); }
    List<StockRequest> selectBranch(@Param("branchId") Long branchId, @Param("status") StockRequestStatus status, @Param("offset") long offset, @Param("limit") int limit);
    long countBranch(@Param("branchId") Long branchId, @Param("status") StockRequestStatus status);

    // --- 蹂몄궗 ?붾㈃: ??吏???좎껌 寃??---

    // ??蹂대궦 議곌굔? null濡??ㅼ뼱???洹?以꾩씠 ?듭㎏濡?李몄씠 ?섎?濡? ?ㅼ젣濡?蹂대궦 議곌굔留?嫄몃윭吏꾨떎.
    // keyword???몄텧遺?먯꽌 ?뚮Ц??+ %...% ?뺥깭濡?留뚮뱾???섍릿??
    default Page<StockRequest> searchForHq(
            @Param("status") StockRequestStatus status,
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("keyword") String keyword,
            Pageable pageable) { return new PageImpl<>(selectForHq(status, branchId, fromDate, toDate, keyword, pageable.getOffset(), pageable.getPageSize()), pageable, countForHq(status, branchId, fromDate, toDate, keyword)); }
    List<StockRequest> selectForHq(@Param("status") StockRequestStatus status,@Param("branchId") Long branchId,@Param("fromDate") LocalDateTime fromDate,@Param("toDate") LocalDateTime toDate,@Param("keyword") String keyword,@Param("offset") long offset,@Param("limit") int limit);
    long countForHq(@Param("status") StockRequestStatus status,@Param("branchId") Long branchId,@Param("fromDate") LocalDateTime fromDate,@Param("toDate") LocalDateTime toDate,@Param("keyword") String keyword);

    long countByRequestStatus(StockRequestStatus status);

    long countByRequestStatusNotIn(Collection<StockRequestStatus> statuses);

    // --- 諛곗넚 愿由??붾㈃ ---

    // 諛곗넚怨?愿?⑤맂 ?④퀎(異쒓퀬 以鍮?諛곗넚 以??섎졊 ?꾨즺)留?紐⑥븘??蹂몃떎.
    // status瑜??곕줈 二쇰㈃ 洹??곹깭 ?섎굹留? ??二쇰㈃ ?꾨옒 statuses 吏묓빀 ?꾩껜瑜?議고쉶?쒕떎.
    default Page<StockRequest> searchDeliveries(
            @Param("statuses") Collection<StockRequestStatus> statuses,
            @Param("status") StockRequestStatus status,
            @Param("branchId") Long branchId,
            @Param("keyword") String keyword,
            Pageable pageable) { return new PageImpl<>(selectDeliveries(statuses,status,branchId,keyword,pageable.getOffset(),pageable.getPageSize()),pageable,countDeliveries(statuses,status,branchId,keyword)); }
    List<StockRequest> selectDeliveries(@Param("statuses") Collection<StockRequestStatus> statuses,@Param("status") StockRequestStatus status,@Param("branchId") Long branchId,@Param("keyword") String keyword,@Param("offset") long offset,@Param("limit") int limit);
    long countDeliveries(@Param("statuses") Collection<StockRequestStatus> statuses,@Param("status") StockRequestStatus status,@Param("branchId") Long branchId,@Param("keyword") String keyword);

    // 諛곗넚 以묒씤???꾩갑 ?덉젙 ?쒓컖??吏??吏?? 嫄댁닔 - 諛곗넚 愿由??붿빟 移대뱶???대떎.
    long countByRequestStatusAndEstimatedArrivalAtBefore(StockRequestStatus status, LocalDateTime time);

    // ?뱀씤/諛섎젮/諛곗넚/?섎졊?뺤씤? 媛숈? ?좎껌 嫄댁쓣 ???щ엺???숈떆??嫄대뱶由????덉뼱??
    // ?곹깭瑜?諛붽씀湲??꾩뿉 ??硫붿꽌?쒕줈 ?됱쓣 ?좉렇怨??쒖옉?쒕떎.
    Optional<StockRequest> findByIdForUpdate(@Param("id") Long id);
    Optional<StockRequest> findById(Long id);
    long count();
    int insert(StockRequest request);
    int update(StockRequest request);
    default StockRequest save(StockRequest request) { if (request.getStockRequestId() == null) insert(request); else update(request); return request; }
}
