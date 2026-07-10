package com.example.kiosksim.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_item_flavors")
public class OrderItemFlavor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderItemId;
    private Long flavorId;
    private String flavorNameSnapshot;
    private Integer selectOrder;
    private Integer quantity;

    protected OrderItemFlavor() {
    }

    public OrderItemFlavor(Long orderItemId, Long flavorId, String flavorNameSnapshot,
                           Integer selectOrder, Integer quantity) {
        this.orderItemId = orderItemId;
        this.flavorId = flavorId;
        this.flavorNameSnapshot = flavorNameSnapshot;
        this.selectOrder = selectOrder;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public Long getFlavorId() {
        return flavorId;
    }

    public String getFlavorNameSnapshot() {
        return flavorNameSnapshot;
    }

    public Integer getSelectOrder() {
        return selectOrder;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
