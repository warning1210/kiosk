package com.kiosk.kiosk.menu.service;

import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.domain.common.SaleStatus;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.kiosk.event.service.KioskFlavorDiscountService;
import com.kiosk.kiosk.menu.dto.CategoriResponse;
import com.kiosk.kiosk.menu.dto.FlavorResponse;
import com.kiosk.kiosk.menu.dto.ProductResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final KioskFlavorDiscountService kioskFlavorDiscountService;

    public List<CategoriResponse> getCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc().stream()
                .map(CategoriResponse::from)
                .toList();
    }

    // 상품에 진행 중인 SIZE_UP 이벤트(이 상품 -> 다른 상품)가 있으면 사이즈업 정보를 같이 실어 보낸다.
    public List<ProductResponse> getProducts() {
        return productRepository.findByIsVisibleTrueAndSaleStatusOrderByProductNameAsc(SaleStatus.ON_SALE).stream()
                .map(product -> ProductResponse.from(product,
                        kioskFlavorDiscountService.activeSizeUpEventFrom(product.getProductId()).orElse(null)))
                .toList();
    }

    // 본점이 직접 지정한 이달의 맛(MONTHLY_FLAVOR, 전 지점 자동 적용) + 지점이 선택한 맛 할인(FLAVOR_DISCOUNT)을
    // 같이 실어 보내고, 할인이 붙은 맛을 화면 목록 맨 앞에 오도록 정렬한다(그 안에서는 기존처럼 이름순 유지).
    public List<FlavorResponse> getFlavors(Long branchId) {
        Map<Long, Event> activeDiscounts = kioskFlavorDiscountService.activeDiscountsByFlavor(branchId);

        List<Flavor> flavors = flavorRepository.findByIsVisibleTrueAndSaleStatusOrderByFlavorNameAsc(SaleStatus.ON_SALE);
        return flavors.stream()
                .map(flavor -> FlavorResponse.from(flavor, activeDiscounts.get(flavor.getFlavorId())))
                .sorted(Comparator.comparing((FlavorResponse response) -> response.discountType() == null))
                .toList();
    }
}
