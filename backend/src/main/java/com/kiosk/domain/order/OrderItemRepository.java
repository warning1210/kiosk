package com.kiosk.domain.order;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderItemRepository {
    List<OrderItem> findByOrder_OrderIdOrderByOrderItemIdAsc(Long orderId);
    List<OrderItem> findByOrder_OrderIdIn(@Param("orderIds") List<Long> orderIds);
    int insert(OrderItem value);
    int update(OrderItem value);
    default OrderItem save(OrderItem value) { if (value.getOrderItemId() == null) insert(value); else update(value); return value; }
}
