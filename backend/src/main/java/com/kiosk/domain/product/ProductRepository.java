package com.kiosk.domain.product;

import com.kiosk.domain.common.SaleStatus;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductRepository {
    List<Product> findAll();
    Product selectById(Long id);
    default Optional<Product> findById(Long id) { return Optional.ofNullable(selectById(id)); }
    List<Product> findByIsVisibleTrueAndSaleStatusOrderByProductNameAsc(@Param("saleStatus") SaleStatus saleStatus);
    int insert(Product product);
    int update(Product product);
    default Product save(Product product) { if (product.getProductId() == null) insert(product); else update(product); return product; }
}
