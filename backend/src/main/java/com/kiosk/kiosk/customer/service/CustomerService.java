package com.kiosk.kiosk.customer.service;

import com.kiosk.domain.coupon.Coupon;
import com.kiosk.domain.coupon.CouponRepository;
import com.kiosk.domain.coupon.CouponStatus;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerRepository;
import com.kiosk.global.security.MobileNumberCrypto;
import com.kiosk.kiosk.customer.dto.CustomerCouponResponse;
import com.kiosk.kiosk.customer.dto.CustomerResponse;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CouponRepository couponRepository;

    public CustomerResponse getByMobileNumber(String mobileNumber) {
        String mobileNumberHash = MobileNumberCrypto.hash(mobileNumber);
        Customer customer = customerRepository.findByMobileNumberHash(mobileNumberHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 회원이 아닙니다."));

        LocalDateTime now = LocalDateTime.now();
        var coupons = couponRepository.findByCustomer_MobileNumberHashAndCouponStatus(mobileNumberHash, CouponStatus.AVAILABLE)
                .stream()
                .filter(coupon -> coupon.getExpiresAt().isAfter(now))
                .sorted(Comparator.comparing(Coupon::getExpiresAt))
                .map(CustomerCouponResponse::from)
                .toList();

        return CustomerResponse.from(customer, mobileNumber, coupons);
    }
}
