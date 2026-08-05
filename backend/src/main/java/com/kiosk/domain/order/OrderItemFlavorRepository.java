package com.kiosk.domain.order;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemFlavorRepository {
    List<OrderItemFlavor> findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(Long orderItemId);
    int insert(OrderItemFlavor value);
    int update(OrderItemFlavor value);
    default OrderItemFlavor save(OrderItemFlavor value) { if (value.getOrderItemFlavorId() == null) insert(value); else update(value); return value; }
}
