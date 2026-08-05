package com.kiosk.domain.flavor;

import com.kiosk.domain.common.SaleStatus;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlavorRepository {
    List<Flavor> findAll();
    Flavor selectById(Long id);
    default Optional<Flavor> findById(Long id) { return Optional.ofNullable(selectById(id)); }
    List<Flavor> findAllById(@Param("ids") Iterable<Long> ids);
    List<Flavor> findByIsVisibleTrueAndSaleStatusOrderByFlavorNameAsc(@Param("saleStatus") SaleStatus saleStatus);
    int insert(Flavor flavor);
    int update(Flavor flavor);
    default Flavor save(Flavor flavor) { if (flavor.getFlavorId() == null) insert(flavor); else update(flavor); return flavor; }
}
