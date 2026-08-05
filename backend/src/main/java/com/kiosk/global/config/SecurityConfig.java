package com.kiosk.global.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// spring-boot-starter-security가 클래스패스에 있으면 스프링이 기본적으로 모든 요청을
// HTTP Basic으로 잠근다(임의 생성 비밀번호). 이 프로젝트는 인증을 Spring Security 필터체인이
// 아니라 각 컨트롤러에서 직접 BranchAccessService/HqAccessService로 검증하므로,
// authorizeHttpRequests를 아예 걸지 않아 인가는 그대로 각 컨트롤러 책임으로 남긴다.
// CORS는 반드시 여기(Security 필터체인)에서 처리해야 한다 - CorsConfigurationSource 빈이 있으면
// Spring Security의 CorsFilter가 필터체인 맨 앞에서 요청을 먼저 가로채므로, 이 뒤에 있는
// WebMvcConfigurer.addCorsMappings()는 애초에 도달하지 못하고 조용히 무시된다(과거에 CORS 설정이
// WebMvcConfig에도 따로 있었는데, 그게 여기 CORS 설정에 가려져 죽은 코드였던 적이 있었다).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 사설 IP(RFC1918) 세 대역을 전부 허용해야 한다: 192.168.0.0/16, 10.0.0.0/8, 172.16.0.0/12.
    // 앞 두 개는 "*.*"/"*.*.*" 와일드카드 하나로 대역 전체가 커버되지만, 172.16.0.0/12는 두 번째
    // 옥텟이 16~31로 고정된 범위라 와일드카드 하나로 못 줄인다 - "172.16.*.*"만 쓰면 172.20.x.x
    // 같은 팀원 IP는 패턴에 안 걸려서 CORS가 막힌다. 그래서 172.16~172.31을 전부 나열한다.
    private static final List<String> PRIVATE_172_ORIGIN_PATTERNS = IntStream.rangeClosed(16, 31)
            .mapToObj(secondOctet -> "http://172." + secondOctet + ".*.*:*")
            .toList();

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
           //.csrf(csrf -> csrf.disable());
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> originPatterns = new ArrayList<>(List.of(
                "http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*", "http://10.*.*.*:*"));
        originPatterns.addAll(PRIVATE_172_ORIGIN_PATTERNS);

        CorsConfiguration configuration = new CorsConfiguration();
        // setAllowedOrigins는 정확히 일치하는 origin만 받는다 - 와일드카드 패턴을 쓰려면
        // setAllowedOriginPatterns를 써야 한다 (허용 범위를 IP 패턴 여러 개로 표현해야 하는 이유).
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
