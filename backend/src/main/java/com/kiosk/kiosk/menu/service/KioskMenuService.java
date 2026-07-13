package com.kiosk.kiosk.menu.service;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.kiosk.menu.dto.MenuCategoryDto;
import com.kiosk.kiosk.menu.dto.MenuFlavorDto;
import com.kiosk.kiosk.menu.dto.MenuProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KioskMenuService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FlavorRepository flavorRepository;

    public List<MenuCategoryDto> getKioskMenus(Long branchId) {
        // 1. 노출 가능한 카테고리 목록 조회 (순서 정렬)
        List<Category> categories = categoryRepository.findByIsVisibleTrueOrderByDisplayOrderAsc();

        // 2. 전체 상품 및 맛 데이터 중 노출 가능한 것만 조회
        // 현재 단계에서는 재고 확인을 생략하기로 했으므로, 지점 아이디(branchId) 기반 필터링은 생략합니다.
        List<Product> products = productRepository.findByIsVisibleTrue();
        List<Flavor> flavors = flavorRepository.findByIsVisibleTrue();

        // 3. 카테고리별로 데이터 그룹화
        return categories.stream().map(category -> {
            List<MenuProductDto> categoryProducts = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getCategoryId().equals(category.getCategoryId()))
                    .map(MenuProductDto::from)
                    .collect(Collectors.toList());

            List<MenuFlavorDto> categoryFlavors = flavors.stream()
                    .filter(f -> f.getCategory() != null && f.getCategory().getCategoryId().equals(category.getCategoryId()))
                    .map(MenuFlavorDto::from)
                    .collect(Collectors.toList());

            return MenuCategoryDto.of(category, categoryProducts, categoryFlavors);
        }).collect(Collectors.toList());
    }
}
