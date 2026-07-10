package com.example.kiosksim.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long productId;
    private String productNameSnapshot;
    private Integer quantity;
    private Integer unitPrice;
    private Integer lineTotal;
    private String containerType;
    private Integer spoonCount;
    private Integer dryIceMinutes;

    protected OrderItem() {
    }

    public OrderItem(Long orderId, Long productId, String productNameSnapshot, Integer quantity,
                     Integer unitPrice, Integer lineTotal, String containerType,
                     Integer spoonCount, Integer dryIceMinutes) {
        this.orderId = orderId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.containerType = containerType;
        this.spoonCount = spoonCount;
        this.dryIceMinutes = dryIceMinutes;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getUnitPrice() {
        return unitPrice;
    }

    public Integer getLineTotal() {
        return lineTotal;
    }

    public String getContainerType() {
        return containerType;
    }

    public Integer getSpoonCount() {
        return spoonCount;
    }

    public Integer getDryIceMinutes() {
        return dryIceMinutes;
    }
}
