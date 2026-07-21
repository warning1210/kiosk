package com.kiosk.hq.product.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.product.dto.HqProductResponse;
import com.kiosk.hq.product.dto.HqProductUpsertRequest;
import com.kiosk.hq.product.service.HqProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/products")
@RequiredArgsConstructor
public class HqProductController {

    private final HqProductService hqProductService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqProductResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqProductService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HqProductResponse create(@RequestBody HqProductUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqProductService.create(request);
    }

    @PutMapping("/{productId}")
    public HqProductResponse update(@PathVariable Long productId, @RequestBody HqProductUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqProductService.update(productId, request);
    }
}
