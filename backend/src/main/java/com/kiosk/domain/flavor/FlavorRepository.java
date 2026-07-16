package com.kiosk.domain.flavor;

import com.kiosk.domain.common.SaleStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlavorRepository extends JpaRepository<Flavor, Long> {

    List<Flavor> findByIsVisibleTrueAndSaleStatusOrderByFlavorNameAsc(SaleStatus saleStatus);
}
