package com.kiosk.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * httpOnly 쿠키에 담긴 로그인 토큰을 Authorization 헤더인 것처럼 보이게 만든다.
 *
 * <p>인증 검증은 27개 컨트롤러 91곳이 {@code @RequestHeader("Authorization")}으로 받고 있어서,
 * 쿠키 방식으로 바꾸려면 그 전부를 고쳐야 했다. 여기서 한 번 변환해주면 그 아래 코드는
 * 저장 위치가 헤더인지 쿠키인지 알 필요가 없다 - 컨트롤러/서비스는 한 줄도 바뀌지 않는다.
 *
 * <p>헤더가 이미 있으면 손대지 않는다. Firebase로 로그인한 지점은 Firebase SDK가 쥔 ID 토큰을
 * 직접 헤더에 실어 보내는데(우리가 발급하는 값이 아니라 쿠키로 옮길 수 없다), 그 경로가
 * 그대로 살아 있어야 하기 때문이다.
 */
@Component
public class AuthCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            chain.doFilter(request, response);
            return;
        }

        String token = AuthCookie.read(request).orElse(null);
        chain.doFilter(token == null ? request : new BearerFromCookie(request, token), response);
    }

    /** Authorization 헤더만 쿠키 값으로 덮어쓰는 얇은 래퍼. 나머지 헤더는 원본 그대로 통과시킨다. */
    private static final class BearerFromCookie extends HttpServletRequestWrapper {

        private final String bearer;

        private BearerFromCookie(HttpServletRequest request, String token) {
            super(request);
            this.bearer = "Bearer " + token;
        }

        @Override
        public String getHeader(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? bearer : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(bearer))
                    : super.getHeaders(name);
        }

        // 헤더 목록을 훑는 코드(로깅, 프레임워크 내부)가 Authorization을 빠뜨리지 않도록 같이 채워준다.
        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
            if (names.stream().noneMatch(HttpHeaders.AUTHORIZATION::equalsIgnoreCase)) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
