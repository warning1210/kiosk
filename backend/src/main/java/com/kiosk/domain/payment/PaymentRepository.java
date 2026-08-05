package com.kiosk.domain.payment;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRepository {
    Payment selectByOrderId(Long orderId);
    Payment selectByQrToken(String qrToken);
    default Optional<Payment> findByOrder_OrderId(Long orderId) { return Optional.ofNullable(selectByOrderId(orderId)); }
    default Optional<Payment> findByQrToken(String qrToken) { return Optional.ofNullable(selectByQrToken(qrToken)); }
    int insert(Payment value);
    int update(Payment value);
    default Payment save(Payment value) { if (value.getPaymentId() == null) insert(value); else update(value); return value; }
}
