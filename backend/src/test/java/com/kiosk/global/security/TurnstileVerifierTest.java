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
}
