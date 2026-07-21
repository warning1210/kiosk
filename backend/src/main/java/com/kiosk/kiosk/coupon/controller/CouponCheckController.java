package com.kiosk.kiosk.coupon.controller;

import com.kiosk.domain.coupon.Coupon;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerRepository;
import com.kiosk.kiosk.coupon.dto.CouponCheckResponse;
import com.kiosk.kiosk.coupon.service.CouponValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 키오스크 결제 전 "쿠폰 확인" 버튼용 - 결제(OrderService.checkout)와 동일한 검증 규칙을 쓰되,
// 쿠폰을 소진시키지 않고 사용 가능 여부/할인 금액만 미리 확인해준다.
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponCheckController {

    private final CouponValidationService couponValidationService;
    private final CustomerRepository customerRepository;

    @GetMapping("/check")
    public CouponCheckResponse check(@RequestParam String code, @RequestParam(required = false) String mobileNumber) {
        Customer customer = (mobileNumber != null && !mobileNumber.isBlank())
                ? customerRepository.findByMobileNumber(mobileNumber).orElse(null)
                : null;
        Coupon coupon = couponValidationService.validate(code, customer);
        if (coupon == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰 코드를 입력해주세요.");
        }
        return new CouponCheckResponse(coupon.getCouponName(), coupon.getDiscountAmount(), coupon.getExpiresAt());
    }
}
