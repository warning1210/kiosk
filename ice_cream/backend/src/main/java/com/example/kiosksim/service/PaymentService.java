package com.example.kiosksim.service;

import com.example.kiosksim.domain.Member;
import com.example.kiosksim.domain.Payment;
import com.example.kiosksim.domain.KioskOrder;
import com.example.kiosksim.dto.PaymentQrResponse;
import com.example.kiosksim.repository.PaymentRepository;
import com.example.kiosksim.repository.KioskOrderRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Transactional
public class PaymentService {
    private final String apiUrl;
    private final String clientKey;
    private final String secretKey;
    private final String successUrl;
    private final String failUrl;
    private final int qrTimeout;
    private final Gson gson;
    private final PaymentRepository paymentRepository;
    private final MemberService memberService;
    private final KioskOrderRepository kioskOrderRepository;

    public PaymentService(
            @Value("${toss.payments.api.url}") String apiUrl,
            @Value("${toss.payments.client.key}") String clientKey,
            @Value("${toss.payments.secret.key}") String secretKey,
            @Value("${toss.payments.success.url}") String successUrl,
            @Value("${toss.payments.fail.url}") String failUrl,
            @Value("${toss.payments.qr.timeout}") int qrTimeout,
            PaymentRepository paymentRepository,
            MemberService memberService,
            KioskOrderRepository kioskOrderRepository) {
        this.apiUrl = apiUrl;
        this.clientKey = clientKey;
        this.secretKey = secretKey;
        this.successUrl = successUrl;
        this.failUrl = failUrl;
        this.qrTimeout = qrTimeout;
        this.gson = new Gson();
        this.paymentRepository = paymentRepository;
        this.memberService = memberService;
        this.kioskOrderRepository = kioskOrderRepository;
    }

    public PaymentQrResponse generateQrCode(Long orderId, Integer amount) {
        try {
            String orderId_str = "ORD-" + orderId + "-" + System.currentTimeMillis();
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(qrTimeout);
            
            // 결제 페이지 URL (토스페이먼츠 테스트 결제 페이지)
            String checkoutUrl = String.format(
                "https://sandbox-payment.tosspayments.com/login?orderId=%s&amount=%d",
                orderId_str, amount
            );
            
            log.info("QR code generated for order: {}, amount: {}", orderId, amount);
            
            // 프론트엔드에서 QR 코드를 생성하므로 checkoutUrl만 반환
            return new PaymentQrResponse(
                "", // QR 코드는 프론트엔드에서 생성
                orderId,
                amount,
                "PENDING",
                expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                checkoutUrl
            );
        } catch (Exception e) {
            log.error("Failed to generate QR code", e);
            throw new RuntimeException("QR 코드 생성 실패: " + e.getMessage());
        }
    }

    // 결제 완료 처리 (포인트 포함)
    public PaymentConfirmResponse confirmPaymentWithPoints(
            Long orderId, 
            Integer finalAmount, 
            String phoneNumber, 
            Integer usedPoints) {
        try {
            // 1. 주문 조회
            KioskOrder order = kioskOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

            // 2. 회원 조회 또는 생성
            Member member = memberService.getOrCreateMember(phoneNumber);

            // 3. 사용할 포인트 차감
            if (usedPoints > 0) {
                boolean success = memberService.usePoints(member, usedPoints, order);
                if (!success) {
                    throw new RuntimeException("포인트가 부족합니다");
                }
            }

            // 4. 적립될 포인트 계산 (주문 금액의 1%)
            Integer earnedPoints = (int) (finalAmount * 0.01);
            memberService.earnPoints(member, finalAmount, order);

            // 5. Payment 테이블에 결제 정보 저장
            Payment payment = new Payment(orderId, "QR", "PENDING", "", null, finalAmount, null);
            payment.setMember(member);
            payment.setUsedPoints(usedPoints);
            payment.setEarnedPoints(earnedPoints);
            payment.completePayment();  // 상태를 DONE으로 변경
            
            Payment savedPayment = paymentRepository.save(payment);

            log.info("Payment confirmed: orderId={}, amount={}, phoneNumber={}, usedPoints={}, earnedPoints={}",
                    orderId, finalAmount, phoneNumber, usedPoints, earnedPoints);

            return new PaymentConfirmResponse(
                    savedPayment.getId(),
                    orderId,
                    finalAmount,
                    "DONE",
                    member.getAvailablePoints(),
                    earnedPoints,
                    usedPoints
            );
        } catch (Exception e) {
            log.error("Failed to confirm payment", e);
            throw new RuntimeException("결제 처리 실패: " + e.getMessage());
        }
    }

    public PaymentQrResponse confirmPayment(String paymentKey, String orderId, String amount) {
        try {
            String requestBody = createConfirmRequestBody(paymentKey, orderId, amount);
            String response = callTossPaymentsApi("/v1/payments/confirm", requestBody);
            
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "UNKNOWN";
            
            log.info("Payment confirmed: orderId={}, status={}", orderId, status);
            
            return new PaymentQrResponse(
                paymentKey,
                Long.valueOf(orderId.split("-")[1]),
                Integer.valueOf(amount),
                status,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                ""
            );
        } catch (Exception e) {
            log.error("Failed to confirm payment", e);
            throw new RuntimeException("결제 승인 실패: " + e.getMessage());
        }
    }

    private String createConfirmRequestBody(String paymentKey, String orderId, String amount) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("paymentKey", paymentKey);
        requestBody.addProperty("orderId", orderId);
        requestBody.addProperty("amount", amount);
        return requestBody.toString();
    }

    private String callTossPaymentsApi(String endpoint, String requestBody) throws Exception {
        String url = apiUrl + endpoint;
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        
        // Basic Auth: secret_key:
        String auth = secretKey + ":";
        String encodedAuth = Base64.encodeBase64String(auth.getBytes(StandardCharsets.UTF_8));
        httpPost.setHeader("Authorization", "Basic " + encodedAuth);
        httpPost.setHeader("Content-Type", "application/json");
        
        HttpEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        
        StringBuilder response = new StringBuilder();
        httpClient.execute(httpPost, response1 -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response1.getEntity().getContent(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return null;
        });
        
        return response.toString();
    }

    // 응답 DTO 내부 클래스
    public static class PaymentConfirmResponse {
        public Long paymentId;
        public Long orderId;
        public Integer amount;
        public String status;
        public Integer remainingPoints;
        public Integer earnedPoints;
        public Integer usedPoints;

        public PaymentConfirmResponse(Long paymentId, Long orderId, Integer amount, String status,
                                     Integer remainingPoints, Integer earnedPoints, Integer usedPoints) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
            this.remainingPoints = remainingPoints;
            this.earnedPoints = earnedPoints;
            this.usedPoints = usedPoints;
        }
    }
}

