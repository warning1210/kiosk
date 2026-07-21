package com.kiosk.hq.product.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.product.dto.HqFlavorResponse;
import com.kiosk.hq.product.dto.HqFlavorUpsertRequest;
import com.kiosk.hq.product.service.HqFlavorService;
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
@RequestMapping("/api/hq/flavors")
@RequiredArgsConstructor
public class HqFlavorController {

    private final HqFlavorService hqFlavorService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqFlavorResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqFlavorService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HqFlavorResponse create(@RequestBody HqFlavorUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqFlavorService.create(request);
    }

    @PutMapping("/{flavorId}")
    public HqFlavorResponse update(@PathVariable Long flavorId, @RequestBody HqFlavorUpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqFlavorService.update(flavorId, request);
    }
}
