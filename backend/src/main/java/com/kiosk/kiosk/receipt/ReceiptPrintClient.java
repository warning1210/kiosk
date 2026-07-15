package com.kiosk.kiosk.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 강사님 PC에서 돌아가는 프린터 서비스로 "영수증 출력 요청"을 보내는 역할.
 *
 * 구조(Kiosk구조.pdf 참고):
 *   우리 백엔드(Kiosk Server) ── HTTP POST ──▶ 프린터 서비스(:8888) ──▶ 블루투스 프린터
 *
 * 왜 프론트(브라우저)가 아니라 백엔드에서 보내나?
 *   프린터 서비스가 CORS 헤더를 주지 않아서 브라우저가 직접 호출하면 차단된다.
 *   서버끼리(server-to-server) 부르는 건 CORS 제약이 없어 문제없이 동작한다.
 *
 * 프린터가 꺼져 있거나 주소가 틀려도 결제/주문은 정상 진행되어야 하므로,
 * 이 클래스는 어떤 경우에도 예외를 밖으로 던지지 않고 true/false 만 돌려준다.
 */
@Component
public class ReceiptPrintClient {

    private static final Logger log = LoggerFactory.getLogger(ReceiptPrintClient.class);

    // 프린터 서비스 주소. 기본값은 강사님 PC(172.16.15.97:8888)이고,
    // 다른 환경에서는 환경변수 PRINTER_URL 로 바꿔 끼울 수 있다.
    //   예) 집에서 테스트: PRINTER_URL=http://localhost:8888/receipt
    private final String printerUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 프린터가 응답 없을 때 오래 기다리지 않도록 연결 2초 제한을 둔다(빠르게 실패).
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public ReceiptPrintClient(@Value("${printer.url:http://172.16.15.97:8888/receipt}") String printerUrl) {
        this.printerUrl = printerUrl;
    }

    /**
     * 프린터 서비스로 영수증 출력을 요청한다.
     * @return 프린터가 SUCCESS 를 돌려주면 true, 그 외(오프라인/실패)면 false
     */
    public boolean print(PrinterReceiptRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(printerUrl))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() == 200 && response.body().contains("SUCCESS");
            if (!success) {
                log.warn("영수증 출력 실패 응답: status={}, body={}", response.statusCode(), response.body());
            }
            return success;
        } catch (Exception e) {
            // 프린터 서비스가 꺼져 있거나(집에서 개발 등) 네트워크가 안 되는 경우.
            // 주문/결제는 이미 끝났으므로 에러만 로그로 남기고 조용히 false 를 돌려준다.
            log.warn("프린터 서비스에 연결하지 못했습니다 ({}). 영수증은 화면으로만 표시됩니다.", printerUrl);
            return false;
        }
    }
}
