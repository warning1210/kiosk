package com.kiosk.domain.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 특정 주문에 담긴 상품 목록을 담은 순서(id 오름차순)대로 조회한다.
    // 메서드 이름만 규칙에 맞게 지으면 Spring Data JPA 가 SQL 을 자동으로 만들어준다.
    // (order_ 는 OrderItem.order 필드를, OrderId 는 그 안의 orderId 를 가리킴)
    List<OrderItem> findByOrder_OrderIdOrderByOrderItemIdAsc(Long orderId);
}
