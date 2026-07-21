package com.kiosk.kiosk.coupon.service;

import com.kiosk.domain.coupon.Coupon;
import com.kiosk.domain.coupon.CouponRepository;
import com.kiosk.domain.coupon.CouponStatus;
import com.kiosk.domain.customer.Customer;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 쿠폰 코드(qr_token) 검증 - 결제 전 미리보기 확인(CouponCheckController)과 실제 체크아웃(OrderService)이
// 동일한 규칙을 쓰도록 공용으로 둔다. 존재/미사용/미만료/(발급 대상이 있다면) 본인 명의인지까지 확인한다.
// 쿠폰 자체를 소진(USED) 처리하지는 않는다 - 그건 실제 주문을 만드는 쪽의 책임이다.
@Service
@RequiredArgsConstructor
@Transactional
public class CouponValidationService {

    private final CouponRepository couponRepository;

    public Coupon validate(String couponCode, Customer customer) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByQrToken(couponCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 쿠폰입니다."));
        if (coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (coupon.getCouponStatus() == CouponStatus.AVAILABLE) {
                coupon.setCouponStatus(CouponStatus.EXPIRED);
                couponRepository.save(coupon);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "유효기간이 지난 쿠폰입니다.");
        }
        if (coupon.getCouponStatus() != CouponStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다.");
        }
        if (coupon.getCustomer() != null
                && (customer == null || !coupon.getCustomer().getCustomerId().equals(customer.getCustomerId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 명의의 쿠폰이 아닙니다.");
        }
        return coupon;
    }
}
