package com.kiosk.kiosk.payment.toss;
 
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
 
/**
 * application.yml에 아래 내용을 추가하세요.
 *
 * toss:
 *   secret-key: ${TOSS_SECRET_KEY:test_sk_docs_OaPz8L5KdmQXkzRz3y47BMw6}
 *   client-key: ${TOSS_CLIENT_KEY:test_ck_docs_Ovk5rk1EwkEbP0W43n07VZUWBN}
 *   api-base-url: https://api.tosspayments.com
 *   success-url: ${TOSS_SUCCESS_URL:http://localhost:5173/payment/success}
 *   fail-url: ${TOSS_FAIL_URL:http://localhost:5173/payment/fail}
 */
@Configuration
@ConfigurationProperties(prefix = "toss")
public class TossPaymentsProperties {
 
    /** 시크릿 키. 절대 프론트엔드나 깃허브에 노출되면 안 됨 */
    private String secretKey;
 
    /** 클라이언트 키. 프론트엔드에 그대로 내려줘도 되는 값 */
    private String clientKey;
 
    private String apiBaseUrl = "https://api.tosspayments.com";
 
    /** 결제 인증 성공 후 리다이렉트될 프론트엔드 URL */
    private String successUrl;
 
    /** 결제 인증 실패 후 리다이렉트될 프론트엔드 URL */
    private String failUrl;
 
    public String getSecretKey() {
        return secretKey;
    }
 
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
 
    public String getClientKey() {
        return clientKey;
    }
 
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
 
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
 
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
 
    public String getSuccessUrl() {
        return successUrl;
    }
 
    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }
 
    public String getFailUrl() {
        return failUrl;
    }
 
    public void setFailUrl(String failUrl) {
        this.failUrl = failUrl;
    }
}
 