package com.kiosk.kiosk.payment.toss;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
 
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
 
/**
 * 토스페이먼츠 결제 승인 API 호출만 전담하는 게이트웨이.
 * PaymentService는 이 클래스를 통해서만 토스와 통신한다.
 */
@Component
public class TossPaymentGateway {
 
    private final RestTemplate tossRestTemplate;
    private final TossPaymentsProperties tossProperties;
    private final ObjectMapper objectMapper;
 
    public TossPaymentGateway(RestTemplate tossRestTemplate,
                               TossPaymentsProperties tossProperties,
                               ObjectMapper objectMapper) {
        this.tossRestTemplate = tossRestTemplate;
        this.tossProperties = tossProperties;
        this.objectMapper = objectMapper;
    }
 
    /**
     * 결제 승인 API 호출. 성공하면 Payment 객체(JSON)를 그대로 반환한다.
     */
    public JsonNode confirm(String paymentKey, String orderId, Integer amount) {
        String url = tossProperties.getApiBaseUrl() + "/v1/payments/confirm";
 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader());
 
        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);
 
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);
 
        try {
            ResponseEntity<JsonNode> response =
                    tossRestTemplate.postForEntity(url, httpRequest, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw toTossPaymentException(e);
        }
    }
 
    private String buildBasicAuthHeader() {
        String encoded = Base64.getEncoder()
                .encodeToString((tossProperties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
 
    private TossPaymentException toTossPaymentException(Exception e) {
        String responseBody;
        int statusCode;
 
        if (e instanceof HttpClientErrorException httpClientErrorException) {
            responseBody = httpClientErrorException.getResponseBodyAsString();
            statusCode = httpClientErrorException.getStatusCode().value();
        } else {
            HttpServerErrorException httpServerErrorException = (HttpServerErrorException) e;
            responseBody = httpServerErrorException.getResponseBodyAsString();
            statusCode = httpServerErrorException.getStatusCode().value();
        }
 
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
 
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody);
            String code = errorNode.path("code").asText("UNKNOWN_ERROR");
            String message = errorNode.path("message").asText(e.getMessage());
            return new TossPaymentException(status, code, message);
        } catch (Exception parseException) {
            return new TossPaymentException(status, "UNKNOWN_ERROR", e.getMessage());
        }
    }
}