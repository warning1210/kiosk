package com.kiosk.global.config;

import com.kiosk.global.security.CurrentAdminArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;

    public WebMvcConfig(CurrentAdminArgumentResolver currentAdminArgumentResolver) {
        this.currentAdminArgumentResolver = currentAdminArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminArgumentResolver);
    }
}
