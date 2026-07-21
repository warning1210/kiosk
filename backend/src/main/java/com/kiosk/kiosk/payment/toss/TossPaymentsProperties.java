package com.kiosk.kiosk.payment.toss;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yml에 아래 내용을 추가하세요.
 *
 * toss:
 *   secret-key: ${TOSS_SECRET_KEY:test_sk_docs_OaPz8L5KdmQXkzRz3y47BMw6}
 *   client-key: ${TOSS_CLIENT_KEY:test_ck_docs_Ovk5rk1EwkEbP0W43n07VZUWBN}
 *   api-base-url: https://api.tosspayments.com
 *   success-url: ${TOSS_SUCCESS_URL:}
 *   fail-url: ${TOSS_FAIL_URL:}
 *
 * success-url/fail-url을 비워두면 이 서버 PC의 LAN IP를 자동으로 잡아서
 * http://<LAN IP>:5173/payment/... 로 쓴다. 결제 승인 후 토스가 리다이렉트시키는 대상은
 * "손님 폰 브라우저"라서, 여기 IP를 고정값(localhost나 특정 PC의 IP)으로 박아두면
 * 서버가 다른 네트워크에서 돌 때 폰이 그 주소에 접속하지 못해 결제 확인이 영영 안 들어오고
 * 키오스크 화면은 폴링만 계속 돌며 무한 로딩에 빠진다. ngrok 등 공인 주소로 데모할 땐
 * TOSS_SUCCESS_URL/TOSS_FAIL_URL 환경변수로 덮어쓸 것.
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
        return (successUrl == null || successUrl.isBlank()) ? lanUrl("/payment/success") : successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getFailUrl() {
        return (failUrl == null || failUrl.isBlank()) ? lanUrl("/payment/fail") : failUrl;
    }

    public void setFailUrl(String failUrl) {
        this.failUrl = failUrl;
    }

    private static String lanUrl(String path) {
        return "http://" + detectLanIp() + ":5173" + path;
    }

    // 이 PC의 LAN IP(예: 192.168.x.x)를 찾는다. 여러 개면 첫 번째로 찾은 것을 쓴다 -
    // VPN 등 다른 어댑터가 먼저 잡히면 안 맞을 수 있으니, 그런 환경에선 TOSS_SUCCESS_URL/FAIL_URL로 직접 지정할 것.
    private static String detectLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // 네트워크 인터페이스 조회 실패 - 아래에서 localhost로 폴백
        }
        return "localhost";
    }
}
