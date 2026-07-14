package com.kiosk.kiosk.payment.toss;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TossRestTemplateConfig {

    @Bean
    public RestTemplate tossRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(65_000); // 토스 승인 API는 최대 60초까지 걸릴 수 있음
        return new RestTemplate(factory);
    }
}
