package com.kiosk.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 본점 관리자용 경량 서명 토큰. Firebase처럼 서버 세션을 저장하지 않고
 * adminId+만료시각을 HMAC-SHA256으로 서명해서 stateless로 검증한다.
 */
@Component
public class AdminTokenService {

    private static final Duration VALID_DURATION = Duration.ofHours(12);

    private final SecretKeySpec key;

    public AdminTokenService(@Value("${hq.token-secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("HQ_TOKEN_SECRET은 32바이트 이상의 값으로 설정해야 합니다.");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String issue(Long adminId) {
        long expiresAt = Instant.now().plus(VALID_DURATION).getEpochSecond();
        String payload = adminId + ":" + expiresAt;
        String raw = payload + ":" + sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 유효한 토큰이면 adminId를, 아니면 null을 반환한다. */
    public Long verify(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 3) return null;

            String payload = parts[0] + ":" + parts[1];
            if (!sign(payload).equals(parts[2])) return null;

            long expiresAt = Long.parseLong(parts[1]);
            if (Instant.now().getEpochSecond() > expiresAt) return null;

            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 서명에 실패했습니다.", e);
        }
    }
}
