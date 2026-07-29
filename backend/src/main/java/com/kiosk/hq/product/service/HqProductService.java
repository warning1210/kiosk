package com.kiosk.hq.product.service;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.domain.common.SaleStatus;
import com.kiosk.domain.product.ContainerPolicy;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.hq.product.dto.HqProductResponse;
import com.kiosk.hq.product.dto.HqProductUpsertRequest;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HqProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<HqProductResponse> list() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getProductId).reversed())
                .map(HqProductResponse::from)
                .toList();
    }

    public HqProductResponse create(HqProductUpsertRequest request) {

        Product product = Product.builder()
                .category(resolveCategory(request.categoryId()))
                .productName(request.productName())
                .basePrice(request.basePrice())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .requiresFlavorSelection(Boolean.TRUE.equals(request.requiresFlavorSelection()))
                .selectableFlavorCount(toByte(request.selectableFlavorCount()))
                .containerPolicy(
                        parseEnum(ContainerPolicy.class, request.containerPolicy(), "용기 정책", ContainerPolicy.NONE))
                .isLarge(Boolean.TRUE.equals(request.isLarge()))
                .isNew(Boolean.TRUE.equals(request.isNew()))
                .saleStatus(parseEnum(SaleStatus.class, request.saleStatus(), "판매 상태", SaleStatus.ON_SALE))
                .isVisible(request.isVisible() == null || request.isVisible())
                .build();

        return HqProductResponse.from(productRepository.save(product));
    }

    public HqProductResponse update(Long productId, HqProductUpsertRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        product.setCategory(resolveCategory(request.categoryId()));
        product.setProductName(request.productName());
        product.setBasePrice(request.basePrice());
        product.setImageUrl(request.imageUrl());
        product.setDescription(request.description());
        product.setRequiresFlavorSelection(Boolean.TRUE.equals(request.requiresFlavorSelection()));
        product.setSelectableFlavorCount(toByte(request.selectableFlavorCount()));
        product.setContainerPolicy(
                parseEnum(ContainerPolicy.class, request.containerPolicy(), "용기 정책", ContainerPolicy.NONE));
        product.setIsLarge(Boolean.TRUE.equals(request.isLarge()));
        product.setIsNew(Boolean.TRUE.equals(request.isNew()));
        product.setSaleStatus(parseEnum(SaleStatus.class, request.saleStatus(), "판매 상태", SaleStatus.ON_SALE));
        product.setIsVisible(request.isVisible() == null || request.isVisible());

        return HqProductResponse.from(product);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    private Byte toByte(Integer value) {
        return value == null ? 0 : value.byteValue();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String fieldLabel, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 " + fieldLabel + "입니다: " + value);
        }
    }
}
