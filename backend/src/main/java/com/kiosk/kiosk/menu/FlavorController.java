package com.kiosk.kiosk.menu;

import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flavors")
public class FlavorController {

    private final FlavorRepository flavorRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository inventoryRepository;

    public FlavorController(FlavorRepository flavorRepository, ProductRepository productRepository,
                            BranchRepository branchRepository, BranchInventoryRepository inventoryRepository) {
        this.flavorRepository = flavorRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public List<FlavorResponse> getFlavors() {
        List<Flavor> flavors = flavorRepository.findBySourceUrlIsNotNullAndIsVisibleTrueOrderByFlavorIdAsc();
        branchRepository.findFirstByOrderByBranchIdAsc().ifPresent(branch -> {
            List<Long> visibleIds = inventoryRepository.findKioskVisibleFlavorIds(branch.getBranchId());
            if (!visibleIds.isEmpty()) flavors.removeIf(flavor -> !visibleIds.contains(flavor.getFlavorId()));
        });
        List<Long> popularIds = flavorRepository.findPopularFlavorIds().stream()
                .filter(id -> flavors.stream().anyMatch(flavor -> flavor.getFlavorId().equals(id) && !isMonthlyFlavor(flavor.getFlavorName())))
                .limit(5)
                .toList();
        if (popularIds.isEmpty()) {
            List<String> fallbackNames = List.of("엄마는 외계인", "아몬드 봉봉", "민트 초콜릿 칩", "레인보우 샤베트", "뉴욕 치즈케이크");
            popularIds = fallbackNames.stream()
                    .flatMap(name -> flavors.stream().filter(flavor -> flavor.getFlavorName().equals(name)).limit(1))
                    .map(Flavor::getFlavorId)
                    .toList();
        }
        Map<Long, Integer> ranks = new HashMap<>();
        for (int index = 0; index < popularIds.size(); index++) ranks.put(popularIds.get(index), index + 1);
        return flavors.stream()
                .sorted(Comparator
                        .comparingInt((Flavor flavor) -> isMonthlyFlavor(flavor.getFlavorName()) ? 0 : 1)
                        .thenComparingInt(flavor -> ranks.getOrDefault(flavor.getFlavorId(), 999))
                        .thenComparing(Flavor::getFlavorId))
                .map(flavor -> FlavorResponse.from(flavor, isMonthlyFlavor(flavor.getFlavorName()), ranks.get(flavor.getFlavorId())))
                .toList();
    }

    private boolean isMonthlyFlavor(String flavorName) {
        return "우디의 후르츠 어드벤처".equals(flavorName)
                || "쵸파의 코튼캔디 크런치".equals(flavorName);
    }

    @GetMapping("/sizes")
    public List<ProductSizeResponse> getIceCreamSizes() {
        Map<String, Integer> displayOrder = Map.of(
                "싱글레귤러", 1,
                "싱글킹", 2,
                "더블주니어", 3,
                "더블레귤러", 4,
                "파인트", 5,
                "쿼터", 6,
                "패밀리", 7,
                "하프갤런", 8
        );
        Map<String, Product> uniqueProducts = new LinkedHashMap<>();
        productRepository.findByRequiresFlavorSelectionTrueAndIsVisibleTrueOrderByProductIdDesc()
                .forEach(product -> uniqueProducts.putIfAbsent(product.getProductName(), product));
        return uniqueProducts.values().stream()
                .filter(product -> displayOrder.containsKey(product.getProductName()))
                .sorted(Comparator.comparingInt(product -> displayOrder.get(product.getProductName())))
                .map(ProductSizeResponse::from)
                .toList();
    }
}
