package com.kiosk.hq.product.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.product.dto.HqProductResponse;
import com.kiosk.hq.product.dto.HqProductUpsertRequest;
import com.kiosk.hq.product.service.HqProductService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<?> create(@RequestBody @Valid HqProductUpsertRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);

        // 유효성 검증 실패 시 필드별 에러 메시지 반환
        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errorMap.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(hqProductService.create(request));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> update(@PathVariable Long productId,
            @RequestBody @Valid HqProductUpsertRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);

        // 유효성 검증 실패 시 필드별 에러 메시지 반환
        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errorMap.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }

        return ResponseEntity.ok(hqProductService.update(productId, request));
    }
}
