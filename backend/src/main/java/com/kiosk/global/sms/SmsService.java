package com.kiosk.global.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// ponytail: SOLAPI 발신번호/API 키 발급 전까지 실제 발송 대신 로그로 시뮬레이션.
// 키 발급되면 이 메서드 안쪽만 SOLAPI REST 호출로 교체하면 됨 (호출부는 그대로 유지).
@Slf4j
@Service
public class SmsService {

    public void send(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        log.info("[SMS 시뮬레이션] to={} message={}", phoneNumber, message);
    }
}
