package com.kiosk.hq.product.controller;

import com.kiosk.domain.category.CategoryRepository;
import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.product.dto.HqCategoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/categories")
@RequiredArgsConstructor
public class HqCategoryController {

    private final CategoryRepository categoryRepository;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqCategoryResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return categoryRepository.findAllByOrderByCategoryNameAsc().stream()
                .map(HqCategoryResponse::from)
                .toList();
    }
}
