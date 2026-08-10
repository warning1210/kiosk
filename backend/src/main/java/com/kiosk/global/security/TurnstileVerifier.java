package com.kiosk.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 로그인 폼의 Cloudflare Turnstile 캡차 토큰을 서버에서 검증한다.
 * 프론트에서만 체크하면 /api/*-auth/* 를 직접 호출하는 요청은 캡차 없이 그대로 통과하므로,
 * 각 로그인 서비스가 실제 인증(비밀번호/Firebase 토큰 검증) 이전에 이걸 먼저 호출해야 의미가 있다.
 *
 * 로그인 화면은 본사/지점을 미리 나누지 않고 두 경로를 순서대로 시도하는데(BranchLoginView.login),
 * Turnstile 토큰은 1회용이라 첫 경로가 소모하면 두 번째 경로에서는 같은 토큰이 항상 거부된다.
 * 그래서 검증에 성공한 토큰은 짧은 시간 동안 통과시켜, 한 번의 캡차로 순차 시도가 가능하게 한다.
 */
@Component
public class TurnstileVerifier {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    // 순차 시도(HQ -> 지점)가 끝나기에 충분하면서, 토큰이 오래 재사용되지 않을 만큼 짧게 잡는다.
    private static final Duration REUSE_WINDOW = Duration.ofSeconds(60);

    private final RestTemplate restTemplate;
    private final String secretKey;
    // 이미 Cloudflare 검증을 통과한 토큰과 그 유효 기한.
    private final Map<String, Instant> verifiedUntil = new ConcurrentHashMap<>();

    @Autowired
    public TurnstileVerifier(@Value("${turnstile.secret-key:}") String secretKey) {
        this(new RestTemplate(), secretKey);
    }

    // MockRestServiceServer를 붙여 실제 Cloudflare 호출 없이 테스트하기 위한 패키지 전용 생성자.
    TurnstileVerifier(RestTemplate restTemplate, String secretKey) {
        this.restTemplate = restTemplate;
        this.secretKey = secretKey;
    }

    public void verify(String token) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("TURNSTILE_SECRET_KEY가 설정되지 않았습니다.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("본인 인증을 먼저 완료해주세요.");
        }

        Instant now = Instant.now();
        // 만료된 항목을 정리해 맵이 무한정 커지지 않게 한다(로그인 시도량이 적어 전체 순회로 충분).
        verifiedUntil.values().removeIf(expiresAt -> expiresAt.isBefore(now));
        if (verifiedUntil.containsKey(token)) {
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        JsonNode result;
        try {
            result = restTemplate.postForObject(SITEVERIFY_URL, new HttpEntity<>(form, headers), JsonNode.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("본인 인증 확인 중 오류가 발생했습니다.");
        }

        if (result == null || !result.path("success").asBoolean(false)) {
            throw new IllegalArgumentException("본인 인증에 실패했습니다. 다시 시도해주세요.");
        }

        verifiedUntil.put(token, now.plus(REUSE_WINDOW));
    }
}
