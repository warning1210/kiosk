package com.kiosk.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 로그인류 엔드포인트(HQ/지점 로그인, Firebase 세션 교환)에 대해 클라이언트 IP 기준으로
// 짧은 시간 안에 너무 많은 요청이 오면 429로 막는다. Turnstile은 "사람인지"만 걸러낼 뿐
// 같은 사람의 무제한 재시도(무차별 대입)까지 막지는 못하므로 그 방어선을 여기서 채운다.
// 같은 매장 공유기 뒤에 여러 키오스크가 물려 있을 수 있어 계정이 아니라 IP로 거는 만큼
// 매장 전체가 한 번에 묶이지 않도록 임계치를 넉넉하게 잡았다.
// Spring Boot가 Filter 빈을 자동으로 서블릿 컨테이너에 등록해주므로 별도 설정은 필요 없다.
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/hq-auth/login",
            "/api/branch-auth/db-login",
            "/api/branch-auth/firebase-session"
    );

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private record Window(int count, Instant resetAt) {}

    private final ConcurrentHashMap<String, AtomicReference<Window>> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        AtomicReference<Window> ref = attempts.computeIfAbsent(ip,
                k -> new AtomicReference<>(new Window(0, Instant.now().plus(WINDOW))));

        Window updated = ref.updateAndGet(w -> Instant.now().isAfter(w.resetAt())
                ? new Window(1, Instant.now().plus(WINDOW))
                : new Window(w.count() + 1, w.resetAt()));

        if (updated.count() > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
