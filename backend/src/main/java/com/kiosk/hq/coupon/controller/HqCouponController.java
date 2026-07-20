package com.kiosk.hq.coupon.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.coupon.dto.HqCouponIssueRequest;
import com.kiosk.hq.coupon.dto.HqCouponResponse;
import com.kiosk.hq.coupon.service.HqCouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/coupons")
@RequiredArgsConstructor
public class HqCouponController {

    private final HqCouponService hqCouponService;
    private final HqAccessService hqAccessService;

    @GetMapping
    public List<HqCouponResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqCouponService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<HqCouponResponse> issue(@RequestBody HqCouponIssueRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return hqCouponService.issue(request);
    }
}
