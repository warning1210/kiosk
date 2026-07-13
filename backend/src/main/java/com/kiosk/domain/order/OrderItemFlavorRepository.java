package com.kiosk.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemFlavorRepository extends JpaRepository<OrderItemFlavor, Long> {
    List<OrderItemFlavor> findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(Long orderItemId);
}
