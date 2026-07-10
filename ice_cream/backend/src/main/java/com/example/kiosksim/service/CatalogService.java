package com.example.kiosksim.service;

import com.example.kiosksim.dto.CatalogResponse;
import com.example.kiosksim.dto.FlavorResponse;
import com.example.kiosksim.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    private final List<ProductResponse> products = List.of(
            new ProductResponse(1L, 1L, "아이스크림", "싱글레귤러", 3900, true, 1, false),
            new ProductResponse(2L, 1L, "아이스크림", "싱글킹", 4700, true, 1, false),
            new ProductResponse(3L, 1L, "아이스크림", "더블주니어", 5400, true, 2, false),
            new ProductResponse(4L, 1L, "아이스크림", "더블레귤러", 7300, true, 2, false),
            new ProductResponse(5L, 1L, "아이스크림", "파인트", 9800, true, 3, true),
            new ProductResponse(6L, 1L, "아이스크림", "쿼터", 18500, true, 4, true),
            new ProductResponse(7L, 1L, "아이스크림", "패밀리", 26000, true, 5, true),
            new ProductResponse(8L, 1L, "아이스크림", "하프갤런", 31500, true, 6, true),
            new ProductResponse(9L, 2L, "커피", "아메리카노", 4500, false, 0, false),
            new ProductResponse(10L, 3L, "음료", "블라스트", 5500, false, 0, false)
    );

    private final List<FlavorResponse> flavors = List.of(
            new FlavorResponse(1L, "엄마는 외계인"),
            new FlavorResponse(2L, "민트초코"),
            new FlavorResponse(3L, "아몬드 봉봉"),
            new FlavorResponse(4L, "사랑에 빠진 딸기"),
            new FlavorResponse(5L, "뉴욕 치즈케이크"),
            new FlavorResponse(6L, "초콜릿 무스"),
            new FlavorResponse(7L, "바닐라"),
            new FlavorResponse(8L, "레인보우 샤베트")
    );

    private final Map<Long, ProductResponse> productMap = products.stream()
            .collect(Collectors.toUnmodifiableMap(ProductResponse::id, Function.identity()));

    private final Map<Long, FlavorResponse> flavorMap = flavors.stream()
            .collect(Collectors.toUnmodifiableMap(FlavorResponse::id, Function.identity()));

    public CatalogResponse getCatalog() {
        return new CatalogResponse(products, flavors);
    }

    public ProductResponse requireProduct(Long productId) {
        ProductResponse product = productMap.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("존재하지 않는 제품입니다: " + productId);
        }
        return product;
    }

    public FlavorResponse requireFlavor(Long flavorId) {
        FlavorResponse flavor = flavorMap.get(flavorId);
        if (flavor == null) {
            throw new IllegalArgumentException("존재하지 않는 맛입니다: " + flavorId);
        }
        return flavor;
    }
}
