package com.kiosk.hq.coupon.service;

import com.kiosk.domain.coupon.Coupon;
import com.kiosk.domain.coupon.CouponDiscountType;
import com.kiosk.domain.coupon.CouponRepository;
import com.kiosk.domain.coupon.CouponStatus;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerGrade;
import com.kiosk.domain.customer.CustomerRepository;
import com.kiosk.hq.coupon.dto.HqCouponIssueRequest;
import com.kiosk.hq.coupon.dto.HqCouponResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 본점 쿠폰 발급 - 등급을 고르면 그 등급의 고객 전원에게 쿠폰을 1장씩 만들어준다.
// 실제 알림 발송(FCM/SMS)은 범위 밖이라, 발급된 쿠폰 값(qrToken)은 이 화면에서 본점이 직접 확인/전달한다.
@Service
@RequiredArgsConstructor
@Transactional
public class HqCouponService {

    private final CouponRepository couponRepository;
    private final CustomerRepository customerRepository;

    public List<HqCouponResponse> issue(HqCouponIssueRequest request) {
        CouponDiscountType discountType;
        try {
            discountType = CouponDiscountType.valueOf(request.discountType());
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 할인 유형입니다: " + request.discountType());
        }
        if (discountType == CouponDiscountType.RATE) {
            if (request.discountRate() == null || request.discountRate().compareTo(BigDecimal.ZERO) <= 0
                    || request.discountRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("할인율은 0보다 크고 100 이하여야 합니다.");
            }
        } else if (request.discountAmount() == null || request.discountAmount() <= 0) {
            throw new IllegalArgumentException("할인 금액을 올바르게 입력해주세요.");
        }
        if (request.expiresAt() == null) {
            throw new IllegalArgumentException("만료 일시를 입력해주세요.");
        }

        List<Customer> targets;
        if ("ALL".equals(request.grade())) {
            targets = customerRepository.findAll();
        } else {
            CustomerGrade grade;
            try {
                grade = CustomerGrade.valueOf(request.grade());
            } catch (Exception e) {
                throw new IllegalArgumentException("올바르지 않은 고객 등급입니다: " + request.grade());
            }
            targets = customerRepository.findByGrade(grade);
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("해당 등급의 고객이 없습니다.");
        }

        List<Coupon> coupons = targets.stream()
                .map(customer -> Coupon.builder()
                        .couponName(request.couponName())
                        .qrToken(UUID.randomUUID().toString())
                        .discountType(discountType)
                        .discountRate(discountType == CouponDiscountType.RATE ? request.discountRate() : null)
                        .discountAmount(discountType == CouponDiscountType.AMOUNT ? request.discountAmount() : null)
                        .couponStatus(CouponStatus.AVAILABLE)
                        .customer(customer)
                        .expiresAt(request.expiresAt())
                        .build())
                .toList();

        return couponRepository.saveAll(coupons).stream().map(HqCouponResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HqCouponResponse> list() {
        return couponRepository.findAllByOrderByCouponIdDesc().stream()
                .map(HqCouponResponse::from)
                .toList();
    }
}
