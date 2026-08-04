package com.kiosk.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 폼의 Cloudflare Turnstile 캡차 토큰을 서버에서 검증한다.
 * 프론트에서만 체크하면 /api/*-auth/* 를 직접 호출하는 요청은 캡차 없이 그대로 통과하므로,
 * 각 로그인 서비스가 실제 인증(비밀번호/Firebase 토큰 검증) 이전에 이걸 먼저 호출해야 의미가 있다.
 */
@Component
public class TurnstileVerifier {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestTemplate restTemplate;
    private final String secretKey;
    private final ConcurrentHashMap<String, Long> verifiedTokens = new ConcurrentHashMap<>();

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

        long now = System.currentTimeMillis();
        // 10회 호출마다 오래된 캐시 정리
        if (now % 10 == 0) {
            verifiedTokens.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        }

        // 2초 이내에 동일한 토큰으로 다시 검증 요청이 오면 (프론트엔드의 본점/지점 순차 로그인 시도 등)
        // Cloudflare를 다시 찌르지 않고 (어차피 1회용이라 실패함) 바로 성공 처리한다.
        if (verifiedTokens.containsKey(token) && now - verifiedTokens.get(token) < 2000) {
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
        
        // 검증 성공 시 토큰을 캐시에 저장
        verifiedTokens.put(token, now);
    }
}
