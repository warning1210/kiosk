package com.kiosk.domain.coupon;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByQrToken(String qrToken);

    Optional<Coupon> findByUsedOrder_OrderId(Long orderId);

    List<Coupon> findAllByOrderByCouponIdDesc();
}
