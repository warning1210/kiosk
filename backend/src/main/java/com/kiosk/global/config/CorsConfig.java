/**
 * 공통 설정: SecurityConfig, WebConfig 등
 */
package com.kiosk.global.config;
 
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
 
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // "*" 불가, 정확한 origin 필요
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true); // 이게 없으면 쿠키가 아예 전송 안 됨
    }
}
 
