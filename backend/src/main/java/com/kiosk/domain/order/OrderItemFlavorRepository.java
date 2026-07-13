package com.kiosk.domain.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemFlavorRepository extends JpaRepository<OrderItemFlavor, Long> {
    List<OrderItemFlavor> findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(Long orderItemId);
}
