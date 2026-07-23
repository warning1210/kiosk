package com.kiosk.kiosk.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiosk.domain.coupon.CouponRepository;
import com.kiosk.domain.coupon.CouponStatus;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentMethod;
import com.kiosk.domain.payment.PaymentRepository;
import com.kiosk.domain.payment.PaymentStatus;
import com.kiosk.branch.inventory.service.BranchInventoryService;
import com.kiosk.kiosk.payment.dto.PaymentCheckoutResponse;
import com.kiosk.kiosk.payment.dto.PaymentQrResponse;
import com.kiosk.kiosk.payment.dto.PaymentStatusResponse;
import com.kiosk.kiosk.payment.dto.TossConfirmRequest;
import com.kiosk.kiosk.payment.toss.TossPaymentGateway;
import com.kiosk.kiosk.payment.toss.TossPaymentsProperties;
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
    private final CouponRepository couponRepository;
    private final BranchInventoryService branchInventoryService;
    private final TossPaymentGateway tossPaymentGateway;
    private final TossPaymentsProperties tossProperties;

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

        return new PaymentQrResponse(order.getOrderId(), payment.getQrToken(), payment.getQrExpiresAt(),
                payment.getRequestedAmount(), tossProperties.getLanBaseUrl());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String qrToken) {
        Payment payment = findByToken(qrToken);
        return toStatusResponse(payment);
    }

    @Transactional
    public PaymentStatusResponse confirm(String qrToken) {
        Payment payment = findByToken(qrToken);
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return toStatusResponse(payment);
        }

        if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }

        payment.setPaidAmount(payment.getRequestedAmount());
        payment.setApprovalNumber("SIM-" + System.currentTimeMillis());
        markPaidAndComplete(payment);

        return toStatusResponse(payment);
    }

    // 결제 페이지(토스 SDK 초기화용) - QR 생성 시 이미 만들어진 Payment 행을 그대로 사용한다.
    @Transactional(readOnly = true)
    public PaymentCheckoutResponse getCheckoutInfo(String qrToken) {
        Payment payment = findByToken(qrToken);
        if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }

        return new PaymentCheckoutResponse(
                qrToken,
                "키오스크 주문 #" + payment.getOrder().getOrderId(),
                payment.getRequestedAmount(),
                tossProperties.getClientKey(),
                tossProperties.getSuccessUrl() + "?qrToken=" + qrToken,
                tossProperties.getFailUrl() + "?qrToken=" + qrToken
        );
    }

    // 토스페이먼츠 successUrl에서 프론트가 호출하는 실제 결제 승인
    @Transactional
    public PaymentStatusResponse confirmWithToss(TossConfirmRequest request) {
        Payment payment = findByToken(request.qrToken());
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return toStatusResponse(payment);
        }

        if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }
        // qrToken을 토스 orderId로 그대로 사용했으므로 반드시 일치해야 함 (위변조 방지)
        if (!request.qrToken().equals(request.orderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 요청 정보가 일치하지 않습니다.");
        }
        // 클라이언트가 보낸 금액과 서버가 들고 있던 실제 결제 금액이 같은지 검증 (금액 조작 방지)
        if (!payment.getRequestedAmount().equals(request.amount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        JsonNode tossPayment = tossPaymentGateway.confirm(request.paymentKey(), request.orderId(), request.amount());

        payment.setPaidAmount(request.amount());
        payment.setPaymentKey(request.paymentKey());
        payment.setApprovalNumber(resolveApprovalNumber(tossPayment, request.paymentKey()));
        markPaidAndComplete(payment);

        return toStatusResponse(payment);
    }

    // 결제 확정 공통 처리: 상태 전환 + 포인트 적립/차감 + 재고 차감
    private void markPaidAndComplete(Payment payment) {
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.PAID);

        Customer customer = order.getCustomer();
        if (customer != null) {
            // 체크아웃 이후 다른 주문 confirm으로 잔액이 줄었을 수 있어 차감 직전 재검증 (잔액 마이너스 방지)
            if (order.getUsedPoints() > customer.getPointBalance()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "포인트 잔액이 부족합니다. 주문을 다시 시도해주세요.");
            }
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

        // 체크아웃 때 이 주문에 연결해둔 쿠폰이 있으면, 결제가 실제로 끝난 지금에서야 소진 처리한다.
        couponRepository.findByUsedOrder_OrderId(order.getOrderId())
                .filter(c -> c.getCouponStatus() == CouponStatus.AVAILABLE)
                .ifPresent(c -> {
                    c.setCouponStatus(CouponStatus.USED);
                    couponRepository.save(c);
                });

        branchInventoryService.deductForOrder(order);
    }

    private String resolveApprovalNumber(JsonNode tossPayment, String paymentKey) {
        if (tossPayment != null && tossPayment.hasNonNull("card")) {
            String approveNo = tossPayment.path("card").path("approveNo").asText(null);
            if (approveNo != null && !approveNo.isBlank()) {
                return approveNo;
            }
        }
        return paymentKey;
    }

    private Payment findByToken(String qrToken) {
        return paymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        return new PaymentStatusResponse(
                payment.getOrder().getOrderId(),
                payment.getPaymentStatus(),
                payment.getRequestedAmount(),
                payment.getPaidAmount(),
                payment.getApprovalNumber(),
                payment.getPaidAt()
        );
    }
}
