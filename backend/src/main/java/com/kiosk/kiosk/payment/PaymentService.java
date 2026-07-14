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
   private final TossPaymentGateway tossPaymentGateway;
   private final TossPaymentsProperties tossProperties;

   
   @Transactional
   public PaymentQrResponse createQr(Long orderId) {
       Order order = orderRepository.findById(orderId)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

       String qrToken = UUID.randomUUID().toString();
       LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(QR_VALID_MINUTES);

       // CU-009-1: 결제 실패 시 QR코드를 재생성하여 다시 시도 - 기존 Payment가 있으면 토큰만 갱신
       // qrToken을 토스 orderId로도 재사용하므로, 재발급될 때마다 토스 입장에서도 새 주문번호가 된다.
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
           payment.setPaymentKey(null); // 이전 시도의 paymentKey 무효화 (Payment 엔티티에 필드 추가 필요)
       }
       payment = paymentRepository.save(payment);

       return new PaymentQrResponse(order.getOrderId(), payment.getQrToken(), payment.getQrExpiresAt(), payment.getRequestedAmount());
   }

   @Transactional(readOnly = true)
   public PaymentStatusResponse getStatus(String qrToken) {
       Payment payment = findByToken(qrToken);
       return toStatusResponse(payment);
   }

   /**
    * QR 스캔 시 열리는 결제 페이지가 토스 SDK 초기화를 위해 필요한 정보 제공
    */
   @Transactional(readOnly = true)
   public PaymentCheckoutResponse getCheckoutInfo(String qrToken) {
       Payment payment = findByToken(qrToken);

       if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
           throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
       }

       Order order = payment.getOrder();

       return new PaymentCheckoutResponse(
               payment.getQrToken(),                       // 토스 orderId로 그대로 사용
               "키오스크 주문 #" + order.getOrderId(),
               payment.getRequestedAmount(),
               tossProperties.getClientKey(),
               tossProperties.getSuccessUrl() + "?qrToken=" + payment.getQrToken(),
               tossProperties.getFailUrl() + "?qrToken=" + payment.getQrToken()
       );
   }

   /**
    * 토스페이먼츠 successUrl에서 프론트가 호출하는 실제 결제 승인.
    * 기존 시뮬레이션 confirm(qrToken)을 대체한다.
    */
   @Transactional
   public PaymentStatusResponse confirmWithToss(TossConfirmRequest request) {
       Payment payment = findByToken(request.qrToken());

       if (payment.getQrExpiresAt().isBefore(LocalDateTime.now())) {
           payment.setPaymentStatus(PaymentStatus.EXPIRED);
           paymentRepository.save(payment);
           throw new ResponseStatusException(HttpStatus.GONE, "QR 유효시간이 지났습니다. 다시 생성해주세요.");
       }

       // qrToken을 orderId로 그대로 사용했으므로 반드시 일치해야 함 (위변조 방지)
       if (!payment.getQrToken().equals(request.orderId())) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 요청 정보가 일치하지 않습니다.");
       }
       // 클라이언트가 보낸 금액과 서버에 저장된 실제 결제 금액이 같은지 검증 (금액 조작 방지)
       if (!payment.getRequestedAmount().equals(request.amount())) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
       }

       JsonNode tossPayment;
       try {
           tossPayment = tossPaymentGateway.confirm(request.paymentKey(), request.orderId(), request.amount());
       } catch (TossPaymentException e) {
           // 재시도할 수 있도록 QR_CREATED로 되돌린다. PaymentStatus에 FAILED 값이 있다면 그걸 사용해도 좋다.
           payment.setPaymentStatus(PaymentStatus.QR_CREATED);
           paymentRepository.save(payment);
           throw new ResponseStatusException(e.getHttpStatus(), e.getMessage());
       }

       payment.setPaymentKey(request.paymentKey()); // Payment 엔티티에 String paymentKey 필드 추가 필요
       payment.setPaymentStatus(PaymentStatus.PAID);
       payment.setPaidAmount(request.amount());
       payment.setPaidAt(LocalDateTime.now());
       payment.setApprovalNumber(resolveApprovalNumber(tossPayment, request.paymentKey()));
       paymentRepository.save(payment);

       applyOrderCompletion(payment);

       return toStatusResponse(payment);
   }

   /**
    * 결제 완료 후 주문 상태 변경 + 포인트 적립/차감 (기존 confirm() 로직 그대로 분리)
    */
   private void applyOrderCompletion(Payment payment) {
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
   }

   private String resolveApprovalNumber(JsonNode tossPayment, String paymentKey) {
       // 카드 결제가 아니면 approveNo가 없을 수 있어 paymentKey로 대체
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
       return new PaymentStatusResponse(payment.getOrder().getOrderId(), payment.getPaymentStatus(), payment.getRequestedAmount());
   }
}