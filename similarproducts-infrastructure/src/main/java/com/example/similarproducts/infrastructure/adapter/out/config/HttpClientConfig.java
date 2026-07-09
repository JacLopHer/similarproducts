package com.example.similarproducts.infrastructure.adapter.out.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(
        RestTemplateBuilder restTemplateBuilder,
        @Value("${external-api.timeout.connect-ms}") long connectTimeoutMs,
        @Value("${external-api.timeout.read-ms}") long readTimeoutMs
    ) {
        return restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(readTimeoutMs))
            .build();
    }
}

