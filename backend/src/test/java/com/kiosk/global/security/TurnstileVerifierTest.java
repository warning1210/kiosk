package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class TurnstileVerifierTest {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void verify_withSuccessResponse_doesNotThrow() {
        server.expect(requestTo(SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        new TurnstileVerifier(restTemplate, "test-secret").verify("valid-token");

        server.verify();
    }

    @Test
    void verify_withFailureResponse_throws() {
        server.expect(requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new TurnstileVerifier(restTemplate, "test-secret").verify("bad-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_withBlankToken_throwsWithoutCallingCloudflare() {
        assertThatThrownBy(() -> new TurnstileVerifier(restTemplate, "test-secret").verify(" "))
                .isInstanceOf(IllegalArgumentException.class);

        server.verify(); // 기대 등록을 안 했으므로 실제 요청이 하나라도 나가면 이 지점에서 실패한다
    }

    @Test
    void verify_withBlankSecretKey_throwsIllegalState() {
        assertThatThrownBy(() -> new TurnstileVerifier(restTemplate, "").verify("some-token"))
                .isInstanceOf(IllegalStateException.class);

        server.verify();
    }

    // 로그인 화면이 본사 -> 지점 순으로 두 경로를 시도하는데 Turnstile 토큰은 1회용이라,
    // 이미 통과한 토큰은 두 번째 호출에서 Cloudflare에 다시 묻지 않고 그대로 통과해야 한다.
    @Test
    void verify_sameTokenTwice_callsCloudflareOnlyOnce() {
        server.expect(org.springframework.test.web.client.ExpectedCount.once(), requestTo(SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        TurnstileVerifier verifier = new TurnstileVerifier(restTemplate, "test-secret");
        verifier.verify("same-token"); // 본사 로그인 시도에서 소모
        verifier.verify("same-token"); // 지점 폴백 로그인 - 캐시로 통과해야 함

        server.verify(); // 기대치가 1회이므로 두 번 호출됐다면 여기서 실패한다
    }

    // 실패한 토큰은 캐시에 남지 않아야 한다 - 남으면 한 번 거부된 토큰이 이후 통과해버린다.
    @Test
    void verify_failedTokenIsNotCached() {
        server.expect(org.springframework.test.web.client.ExpectedCount.twice(), requestTo(SITEVERIFY_URL))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        TurnstileVerifier verifier = new TurnstileVerifier(restTemplate, "test-secret");
        assertThatThrownBy(() -> verifier.verify("bad-token")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify("bad-token")).isInstanceOf(IllegalArgumentException.class);

        server.verify();
    }
}
