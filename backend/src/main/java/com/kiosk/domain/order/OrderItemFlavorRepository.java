package com.kiosk.domain.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemFlavorRepository extends JpaRepository<OrderItemFlavor, Long> {

    // 특정 주문상품에서 고른 맛들을, 고른 순서(selection_order)대로 조회한다.
    List<OrderItemFlavor> findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(Long orderItemId);
}
