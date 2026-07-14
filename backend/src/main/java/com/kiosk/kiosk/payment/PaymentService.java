package com.kiosk.kiosk.payment;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentMethod;
import com.kiosk.domain.payment.PaymentRepository;
import com.kiosk.domain.payment.PaymentStatus;
import com.kiosk.kiosk.payment.toss.TossPaymentException;
import com.kiosk.kiosk.payment.toss.TossPaymentGateway;
import com.kiosk.kiosk.payment.toss.TossPaymentsProperties;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final TossPaymentGateway tossPaymentGateway;
    private final TossPaymentsProperties tossProperties;
 
    // 결제 완료 전까지는 DB에 저장하지 않고 이 메모리 맵에만 들고 있는다.
    // 주의: 서버가 여러 대(수평 확장)로 뜨는 환경이라면 인스턴스마다 메모리가 분리되어 있어
    //       QR을 생성한 서버와 승인을 처리하는 서버가 다르면 동작하지 않는다.
    //       그런 경우엔 이 맵 대신 Redis 같은 공용 캐시를 사용해야 한다.
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();
 
    @Transactional(readOnly = true)
    public PaymentQrResponse createQr(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
 
        String qrToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(QR_VALID_MINUTES);
 
        // CU-009-1: 결제 실패 시 QR코드를 재생성하여 다시 시도
        // -> DB에 저장된 게 없으므로 그냥 새 토큰으로 맵에 다시 넣기만 하면 된다 (기존 토큰은 그대로 만료되도록 둠)
        pendingPayments.put(qrToken, new PendingPayment(orderId, qrToken, order.getFinalAmount(), expiresAt));
 
        return new PaymentQrResponse(orderId, qrToken, expiresAt, order.getFinalAmount());
    }
 
    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String qrToken) {
        PendingPayment pending = pendingPayments.get(qrToken);
 
        if (pending != null) {
            if (pending.isExpired()) {
                pendingPayments.remove(qrToken);
                throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
            }
            // 아직 결제 전이라 DB에는 없는 상태 -> 메모리 정보로 응답
            return new PaymentStatusResponse(pending.orderId(), PaymentStatus.QR_CREATED, pending.amount(), null, null, null);
        }
 
        // 결제가 완료된 건은 이 시점부터 DB에 실제로 존재한다
        Payment payment = paymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        return toStatusResponse(payment);
    }
 
    @Transactional(readOnly = true)
    public PaymentCheckoutResponse getCheckoutInfo(String qrToken) {
        PendingPayment pending = pendingPayments.get(qrToken);
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        }
        if (pending.isExpired()) {
            pendingPayments.remove(qrToken);
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }
 
        return new PaymentCheckoutResponse(
                pending.qrToken(),                          // 토스 orderId로 그대로 사용
                "키오스크 주문 #" + pending.orderId(),
                pending.amount(),
                tossProperties.getClientKey(),
                tossProperties.getSuccessUrl() + "?qrToken=" + pending.qrToken(),
                tossProperties.getFailUrl() + "?qrToken=" + pending.qrToken()
        );
    }
 
    /**
     * 토스페이먼츠 successUrl에서 프론트가 호출하는 실제 결제 승인.
     * 여기서 성공했을 때만 비로소 Payment가 DB에 INSERT된다.
     */
    @Transactional
    public PaymentStatusResponse confirmWithToss(TossConfirmRequest request) {
        PendingPayment pending = pendingPayments.get(request.qrToken());
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        }
        if (pending.isExpired()) {
            pendingPayments.remove(request.qrToken());
            throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
        }
 
        // qrToken을 orderId로 그대로 사용했으므로 반드시 일치해야 함 (위변조 방지)
        if (!pending.qrToken().equals(request.orderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 요청 정보가 일치하지 않습니다.");
        }
        // 클라이언트가 보낸 금액과 서버가 들고 있던 실제 결제 금액이 같은지 검증 (금액 조작 방지)
        if (!pending.amount().equals(request.amount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }
 
        // 토스 승인 실패 시에는 DB에 아무것도 남기지 않는다 (애초에 아직 저장 전이므로)
        JsonNode tossPayment = tossPaymentGateway.confirm(request.paymentKey(), request.orderId(), request.amount());
 
        Order order = orderRepository.findById(pending.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
 
        // 결제 성공이 확정된 이 시점에 딱 한 번 Payment를 생성해서 저장한다
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.QR)
                .paymentStatus(PaymentStatus.PAID)
                .qrToken(pending.qrToken())
                .qrExpiresAt(pending.expiresAt())
                .requestedAmount(pending.amount())
                .paidAmount(request.amount())
                .paidAt(LocalDateTime.now())
                .paymentKey(request.paymentKey())
                .approvalNumber(resolveApprovalNumber(tossPayment, request.paymentKey()))
                .build();
        payment = paymentRepository.save(payment);
 
        applyOrderCompletion(payment);
 
        pendingPayments.remove(request.qrToken());
 
        return toStatusResponse(payment);
    }
 
    private void applyOrderCompletion(Payment payment) {
        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.PAID);
 
        Customer customer = order.getCustomer();
        if (customer != null) {
            if (order.getUsedPoints() > customer.getPointBalance()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "포인트 잔액이 부족합니다. 주문을 다시 시도해주세요.");
            }
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