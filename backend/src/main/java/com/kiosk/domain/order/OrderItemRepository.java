package com.kiosk.domain.order;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemRepository {
    List<OrderItem> findByOrder_OrderIdOrderByOrderItemIdAsc(Long orderId);
    int insert(OrderItem value);
    int update(OrderItem value);
    default OrderItem save(OrderItem value) { if (value.getOrderItemId() == null) insert(value); else update(value); return value; }
}
