package com.kiosk.domain.flavor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FlavorRepository extends JpaRepository<Flavor, Long> {
    List<Flavor> findBySourceUrlIsNotNullAndIsVisibleTrueOrderByFlavorIdAsc();

    @Query(value = """
            SELECT oif.flavor_id
            FROM order_item_flavor oif
            JOIN order_item oi ON oi.order_item_id = oif.order_item_id
            JOIN `order` o ON o.order_id = oi.order_id
            WHERE o.order_status = 'COMPLETED'
            GROUP BY oif.flavor_id
            ORDER BY SUM(oif.quantity) DESC, oif.flavor_id
            LIMIT 10
            """, nativeQuery = true)
    List<Long> findPopularFlavorIds();
}
