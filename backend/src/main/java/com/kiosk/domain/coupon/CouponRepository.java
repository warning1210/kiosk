package com.kiosk.domain.coupon;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponRepository {
    Optional<Coupon> findByQrToken(String qrToken);

    Optional<Coupon> findByUsedOrder_OrderId(Long orderId);

    List<Coupon> findAllByOrderByCouponIdDesc();

    List<Coupon> findByCustomer_MobileNumberHashAndCouponStatus(String mobileNumberHash, CouponStatus couponStatus);
    Optional<Coupon> findById(Long id);
    List<Coupon> findAll();
    int insert(Coupon coupon);
    int update(Coupon coupon);
    default Coupon save(Coupon coupon) { if (coupon.getCouponId() == null) insert(coupon); else update(coupon); return coupon; }
    default List<Coupon> saveAll(List<Coupon> values) { values.forEach(this::save); return values; }
}
