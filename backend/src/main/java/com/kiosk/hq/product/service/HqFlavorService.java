package com.kiosk.hq.product.service;

import com.kiosk.domain.category.Category;
import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.domain.common.SaleStatus;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.hq.product.dto.HqFlavorResponse;
import com.kiosk.hq.product.dto.HqFlavorUpsertRequest;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HqFlavorService {

    private final FlavorRepository flavorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<HqFlavorResponse> list() {
        return flavorRepository.findAll().stream()
                .sorted(Comparator.comparing(Flavor::getFlavorId).reversed())
                .map(HqFlavorResponse::from)
                .toList();
    }

    public HqFlavorResponse create(HqFlavorUpsertRequest request) {
        if (request.flavorName() == null || request.flavorName().isBlank()) {
            throw new IllegalArgumentException("맛 이름을 입력해주세요.");
        }

        Flavor flavor = Flavor.builder()
                .category(resolveCategory(request.categoryId()))
                .flavorName(request.flavorName())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .allergyInfo(request.allergyInfo())
                .saleStatus(parseEnum(request.saleStatus(), SaleStatus.ON_SALE))
                .isVisible(request.isVisible() == null || request.isVisible())
                .build();

        return HqFlavorResponse.from(flavorRepository.save(flavor));
    }

    public HqFlavorResponse update(Long flavorId, HqFlavorUpsertRequest request) {
        Flavor flavor = flavorRepository.findById(flavorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 맛입니다."));

        if (request.flavorName() == null || request.flavorName().isBlank()) {
            throw new IllegalArgumentException("맛 이름을 입력해주세요.");
        }

        flavor.setCategory(resolveCategory(request.categoryId()));
        flavor.setFlavorName(request.flavorName());
        flavor.setImageUrl(request.imageUrl());
        flavor.setDescription(request.description());
        flavor.setAllergyInfo(request.allergyInfo());
        flavor.setSaleStatus(parseEnum(request.saleStatus(), SaleStatus.ON_SALE));
        flavor.setIsVisible(request.isVisible() == null || request.isVisible());

        return HqFlavorResponse.from(flavor);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    private SaleStatus parseEnum(String value, SaleStatus fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return SaleStatus.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 판매 상태입니다: " + value);
        }
    }
}
