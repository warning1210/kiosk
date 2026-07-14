package com.kiosk.domain.stockrequest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link StockRequestItem}의 저장과 조회를 담당하는 Spring Data JPA 저장소이다.
 *
 * <p>{@link JpaRepository}를 상속하면 기본적인 save, findById, findAll 기능은 구현체를 직접 작성하지 않아도
 * 제공된다. 이 인터페이스에는 재고 신청 화면과 상태 변경에서 추가로 필요한 조회만 선언한다.
 */
public interface StockRequestItemRepository extends JpaRepository<StockRequestItem, Long> {

    /**
     * 여러 신청의 상세 항목과 Flavor를 한 번에 읽는다.
     *
     * <p>신청 목록을 응답 DTO로 바꿀 때 항목마다 LAZY Flavor를 따로 조회하면 쿼리가 반복되는 N+1 문제가
     * 생길 수 있다. JOIN FETCH는 이 조회에 한해 항목과 Flavor를 같은 쿼리 결과로 가져온다.
     *
     * @param stockRequestIds 상세 항목을 조회할 StockRequest PK 목록
     * @return 해당 신청에 속한 모든 상세 항목
     */
    @Query("""
            SELECT i FROM StockRequestItem i
            JOIN FETCH i.flavor f
            WHERE i.stockRequest.stockRequestId IN :stockRequestIds
            """)
    List<StockRequestItem> findByStockRequestIdIn(@Param("stockRequestIds") List<Long> stockRequestIds);

    /**
     * 신청 PK 하나에 속한 모든 상세 항목을 조회한다.
     * 메서드 이름의 밑줄은 {@code stockRequest.stockRequestId} 연관관계 경로를 명확히 나눈 것이다.
     */
    List<StockRequestItem> findByStockRequest_StockRequestId(Long stockRequestId);
}
