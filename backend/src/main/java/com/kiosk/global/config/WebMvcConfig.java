package com.kiosk.global.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// HqUploadService가 app.upload.dir에 저장한 파일을 /uploads/** 로 그대로 서빙한다.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    // 개발 중 다른 LAN IP(휴대폰 QR 스캔, 팀원 PC 등)에서 프론트/백엔드에 직접 접근할 때
    // 오리진이 달라져서 CORS가 막히는 걸 막는다. vite 프록시(localhost)는 이 설정이 없어도 되지만,
    // 다른 기기가 백엔드에 직접 요청하면 반드시 필요하다.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://192.168.*.*:*", "http://172.16.*.*:*", "http://10.*.*.*:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
