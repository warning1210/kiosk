package com.kiosk.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 본점 관리자용 경량 서명 토큰. Firebase처럼 서버 세션을 저장하지 않고
 * adminId+토큰세대+만료시각을 HMAC-SHA256으로 서명해서 stateless로 검증한다.
 *
 * <p>무상태 토큰은 한번 나가면 만료 전까지 취소할 수 없다 - 로그아웃을 눌러도 브라우저에서
 * 지워질 뿐 서버가 발급한 토큰은 살아있다. 그래서 payload에 "세대" 번호를 같이 실어 서명하고,
 * 로그아웃 시 그 계정의 세대를 올려 이전 토큰을 전부 무효로 만든다.
 */
@Component
public class AdminTokenService {

    private static final Duration VALID_DURATION = Duration.ofHours(12);
    // SSE는 EventSource가 커스텀 헤더를 못 보내 토큰을 쿼리파라미터로 실어야 하고, 이는 곧
    // 액세스 로그(%r)에 그대로 남는다는 뜻이다 - 그렇다고 12시간짜리 풀 권한 토큰을 URL에 실으면
    // 로그가 새는 순간 그 지점 계정으로 뭐든 할 수 있는 토큰이 유출되는 셈이라, 스트림 연결
    // 직전에만 발급해서 "스트림 구독" 그 자체 외에는 아무 권한도 없는 훨씬 짧은 티켓을 따로 둔다.
    private static final Duration STREAM_TICKET_VALID_DURATION = Duration.ofMinutes(1);
    private static final String STREAM_TICKET_PREFIX = "stream";

    private final SecretKeySpec key;

    // adminId -> 현재 토큰 세대. 없으면 0세대.
    // ponytail: 프로세스 메모리라 서버를 재시작하면 초기화되고, 그때 무효화했던 토큰이 만료 전까지
    // 되살아난다. DB 컬럼(admin.token_version)으로 옮기면 재시작에도 유지되지만 스키마 변경과
    // 팀 전체 마이그레이션이 따라온다 - 재시작 후에도 확실히 죽어야 하는 요구가 생기면 그때 옮긴다.
    private final Map<Long, Integer> tokenVersions = new ConcurrentHashMap<>();

    public AdminTokenService(@Value("${hq.token-secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("HQ_TOKEN_SECRET은 32바이트 이상의 값으로 설정해야 합니다.");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** 이 관리자에게 발급된 모든 토큰을 즉시 무효화한다(로그아웃, 탈취 대응). */
    public void revokeAll(Long adminId) {
        tokenVersions.merge(adminId, 1, Integer::sum);
    }

    public String issue(Long adminId) {
        return sealed(adminId + ":" + currentVersion(adminId) + ":" + expiresIn(VALID_DURATION));
    }

    /** SSE 연결 직전에만 쓰는 1분짜리 전용 티켓 - 이걸로는 스트림 구독 외 다른 API를 못 부른다. */
    public String issueStreamTicket(Long adminId) {
        return sealed(STREAM_TICKET_PREFIX + ":" + adminId + ":" + currentVersion(adminId)
                + ":" + expiresIn(STREAM_TICKET_VALID_DURATION));
    }

    /** 유효한 토큰이면 adminId를, 아니면(위조·만료·로그아웃됨) null을 반환한다. */
    public Long verify(String token) {
        // 세대가 없던 구버전 토큰은 조각이 3개라 여기서 걸러진다 - 배포 시점에 전부 재로그인.
        return resolve(token, 4, 0);
    }

    /**
     * issueStreamTicket()이 만든 티켓만 통과시킨다. 일반 로그인 토큰은 접두사가 달라 여기서 걸러지고,
     * 이 티켓은 반대로 verify()의 4토막 형식이 아니라 일반 API 인증에도 쓸 수 없다.
     */
    public Long verifyStreamTicket(String ticket) {
        return resolve(ticket, 5, 1);
    }

    /**
     * 서명·만료·세대를 모두 확인하고 adminId를 돌려준다.
     *
     * @param expectedParts 조각 수. 일반 토큰은 4(adminId:세대:만료:서명),
     *                      스트림 티켓은 5("stream":adminId:세대:만료:서명)
     * @param offset        adminId가 있는 위치. 티켓은 접두사 한 칸만큼 뒤로 밀린다.
     */
    private Long resolve(String value, int expectedParts, int offset) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != expectedParts) return null;
            if (offset == 1 && !STREAM_TICKET_PREFIX.equals(parts[0])) return null;

            int last = expectedParts - 1;
            String payload = String.join(":", java.util.Arrays.copyOf(parts, last));
            // 서명 비교는 상수시간으로 한다 - equals()는 앞에서부터 다른 바이트가 나오는 순간 반환해서
            // 응답 시간 차이로 올바른 서명을 한 바이트씩 알아낼 여지를 남긴다.
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8),
                    parts[last].getBytes(StandardCharsets.UTF_8))) return null;

            if (Instant.now().getEpochSecond() > Long.parseLong(parts[offset + 2])) return null;

            Long adminId = Long.parseLong(parts[offset]);
            // 로그아웃 등으로 세대가 올라갔으면 서명이 멀쩡해도 죽은 토큰이다.
            if (Integer.parseInt(parts[offset + 1]) != currentVersion(adminId)) return null;

            return adminId;
        } catch (Exception e) {
            return null;
        }
    }

    private int currentVersion(Long adminId) {
        return tokenVersions.getOrDefault(adminId, 0);
    }

    private long expiresIn(Duration duration) {
        return Instant.now().plus(duration).getEpochSecond();
    }

    private String sealed(String payload) {
        String raw = payload + ":" + sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
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
