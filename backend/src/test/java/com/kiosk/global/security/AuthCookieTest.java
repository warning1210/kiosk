package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 로그인 토큰을 localStorage에서 httpOnly 쿠키로 옮긴 변경의 핵심 두 가지를 확인한다.
 *
 * <p>1) 쿠키에 HttpOnly가 실제로 붙는가 — 이게 빠지면 JS가 그냥 읽을 수 있어서
 * localStorage에 두던 시절과 위험이 똑같아진다.
 * <p>2) 필터가 쿠키를 Authorization 헤더로 바꿔주는가 — 27개 컨트롤러 91곳이
 * {@code @RequestHeader}로 받고 있어서, 이 변환이 없으면 전부 401이 된다.
 */
class AuthCookieTest {

    private final AuthCookie authCookie = new AuthCookie(false);
    private final AuthCookieFilter filter = new AuthCookieFilter();

    @Test
    void issuedCookie_isHttpOnlyAndSameSiteStrict() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authCookie.set(response, "some-token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(AuthCookie.NAME + "=some-token")
                .contains("HttpOnly")        // JS가 document.cookie로 못 읽는다
                .contains("SameSite=Strict") // 다른 사이트에서 온 요청엔 안 실린다(CSRF)
                .contains("Path=/");
    }

    // HTTPS가 붙기 전까지 Secure를 켜면 브라우저가 쿠키를 아예 안 보내 로그인이 통째로 막힌다.
    @Test
    void secureFlag_followsConfiguration() {
        MockHttpServletResponse http = new MockHttpServletResponse();
        MockHttpServletResponse https = new MockHttpServletResponse();

        new AuthCookie(false).set(http, "t");
        new AuthCookie(true).set(https, "t");

        assertThat(http.getHeader(HttpHeaders.SET_COOKIE)).doesNotContain("Secure");
        assertThat(https.getHeader(HttpHeaders.SET_COOKIE)).contains("Secure");
    }

    @Test
    void clear_expiresCookieImmediately() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authCookie.clear(response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void filter_turnsCookieIntoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookie.NAME, "cookie-token"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenAuthorization(chain)).isEqualTo("Bearer cookie-token");
    }

    // Firebase로 로그인한 지점은 Firebase SDK가 쥔 ID 토큰을 직접 헤더에 싣는다 -
    // 우리가 발급하는 값이 아니라 쿠키로 옮길 수 없으므로 그 경로가 그대로 살아 있어야 한다.
    @Test
    void filter_leavesExistingAuthorizationHeaderAlone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer firebase-id-token");
        request.setCookies(new Cookie(AuthCookie.NAME, "cookie-token"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenAuthorization(chain)).isEqualTo("Bearer firebase-id-token");
    }

    @Test
    void filter_withoutCookie_addsNothing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenAuthorization(chain)).isNull();
    }

    // 빈 쿠키(로그아웃 직후 브라우저가 잠깐 들고 있는 값)를 "Bearer "로 만들어 넘기면 안 된다.
    @Test
    void filter_withBlankCookie_addsNothing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookie.NAME, ""));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenAuthorization(chain)).isNull();
    }

    /** 필터를 통과해 컨트롤러에게 실제로 전달된 요청의 Authorization 헤더. */
    private static String seenAuthorization(MockFilterChain chain) {
        return ((jakarta.servlet.http.HttpServletRequest) chain.getRequest()).getHeader(HttpHeaders.AUTHORIZATION);
    }
}
