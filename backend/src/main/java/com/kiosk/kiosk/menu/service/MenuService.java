package com.kiosk.kiosk.menu.service;

import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.domain.common.SaleStatus;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.kiosk.menu.dto.CategoriResponse;
import com.kiosk.kiosk.menu.dto.FlavorResponse;
import com.kiosk.kiosk.menu.dto.ProductResponse;
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
    private final CategoryRepository categoryRepository;

    public List<CategoriResponse> getCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc().stream()
                .map(CategoriResponse::from)
                .toList();
    }

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
