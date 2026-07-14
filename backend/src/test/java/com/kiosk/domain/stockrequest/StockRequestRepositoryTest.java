package com.kiosk.domain.stockrequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

/**
 * Spring Data JPA가 Repository와 사용자 정의 JPQL을 실제로 생성할 수 있는지 확인한다.
 * 서비스 단위 테스트와 달리 H2 메모리 DB와 JPA 설정을 올리는 영속성 계층 테스트다.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StockRequestRepositoryTest {

    @Autowired
    private StockRequestRepository stockRequestRepository;

    @Test
    // 데이터가 없어도 검색 JPQL이 문법 오류 없이 실행되고 빈 페이지를 반환해야 한다.
    void repositoryAndJpqlQueryLoad() {
        // Spring이 Repository 구현체를 자동 생성해 주입했는지 먼저 확인한다.
        assertNotNull(stockRequestRepository);

        // 모든 검색 조건이 null인 가장 넓은 조회가 정상적으로 실행되는지 확인한다.
        assertTrue(stockRequestRepository.searchForHq(
                null, null, null, null, null, PageRequest.of(0, 10)).isEmpty());
    }
}
