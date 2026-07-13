package com.kiosk.domain.stockrequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StockRequestRepositoryTest {

    @Autowired
    private StockRequestRepository stockRequestRepository;

    @Test
    void repositoryAndJpqlQueryLoad() {
        assertNotNull(stockRequestRepository);
        assertTrue(stockRequestRepository.searchForHq(
                null, null, null, null, null, PageRequest.of(0, 10)).isEmpty());
    }
}
