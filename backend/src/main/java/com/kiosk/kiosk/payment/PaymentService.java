package com.kiosk.kiosk.payment;

import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentMethod;
import com.kiosk.domain.payment.PaymentRepository;
import com.kiosk.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int QR_VALID_MINUTES = 5;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentQrResponse createQr(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        String qrToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(QR_VALID_MINUTES);

        // CU-009-1: 결제 실패 시 QR코드를 재생성하여 다시 시도 - 기존 Payment가 있으면 토큰만 갱신
        Payment payment = paymentRepository.findByOrder_OrderId(orderId).orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PaymentMethod.QR)
                    .paymentStatus(PaymentStatus.QR_CREATED)
                    .qrToken(qrToken)
                    .qrExpiresAt(expiresAt)
                    .requestedAmount(order.getFinalAmount())
                    .build();
        } else {
            payment.setQrToken(qrToken);
            payment.setQrExpiresAt(expiresAt);
            payment.setPaymentStatus(PaymentStatus.QR_CREATED);
        }
        payment = paymentRepository.save(payment);

        return new PaymentQrResponse(order.getOrderId(), payment.getQrToken(), payment.getQrExpiresAt(), payment.getRequestedAmount());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String qrToken) {
        Payment payment = findByToken(qrToken);
        return toStatusResponse(payment);
    }

    @Transactional
    public PaymentStatusResponse confirm(String qrToken) {
        Payment payment = findByToken(qrToken);

        if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAmount(payment.getRequestedAmount());
        payment.setPaidAt(LocalDateTime.now());
        payment.setApprovalNumber("SIM-" + System.currentTimeMillis());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.PAID);

        Customer customer = order.getCustomer();
        if (customer != null) {
            // 포인트를 사용한 결제건은 적립 대상에서 제외 (사용과 적립 중복 방지)
            int earnedPoints;
            if (order.getUsedPoints() > 0) {
                earnedPoints = 0;
            } else {
                int earnRatePercent = switch (customer.getGrade()) {
                    case FRIEND -> 3;
                    case FAMILY -> 5;
                    case VIP -> 8;
                };
                earnedPoints = order.getFinalAmount() * earnRatePercent / 100;
            }
            customer.setPointBalance(customer.getPointBalance() - order.getUsedPoints() + earnedPoints);
            order.setEarnedPoints(earnedPoints);
        }
        orderRepository.save(order);

        return toStatusResponse(payment);
    }

    private Payment findByToken(String qrToken) {
        return paymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        return new PaymentStatusResponse(payment.getOrder().getOrderId(), payment.getPaymentStatus(), payment.getRequestedAmount());
    }
}
