package com.kiosk.global.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// HqUploadService가 app.upload.dir에 저장한 파일을 /uploads/** 로 그대로 서빙한다.
// CORS 설정은 여기 두지 않는다 - SecurityConfig의 CorsFilter가 필터체인 앞단에서 먼저 요청을
// 가로채서, WebMvcConfigurer.addCorsMappings()는 있어도 도달하지 못하고 조용히 무시된다.
// CORS를 바꿀 일이 있으면 SecurityConfig.corsConfigurationSource()를 고칠 것.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
