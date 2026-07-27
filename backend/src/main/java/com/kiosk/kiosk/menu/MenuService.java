package com.kiosk.kiosk.menu;

import com.kiosk.domain.common.SaleStatus;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.product.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final ProductRepository productRepository;
    private final FlavorRepository flavorRepository;

    public List<ProductResponse> getProducts() {
        return productRepository.findByIsVisibleTrueAndSaleStatusOrderByProductNameAsc(SaleStatus.ON_SALE).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<FlavorResponse> getFlavors() {
        return flavorRepository.findByIsVisibleTrueAndSaleStatusOrderByFlavorNameAsc(SaleStatus.ON_SALE).stream()
                .map(FlavorResponse::from)
                .toList();
    }
}
