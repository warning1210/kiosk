package com.example.kiosksim.repository;

import com.example.kiosksim.domain.OrderItemFlavor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemFlavorRepository extends JpaRepository<OrderItemFlavor, Long> {
    List<OrderItemFlavor> findByOrderItemId(Long orderItemId);
}
