package com.example.similarproducts.infrastructure.adapter.out.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class HttpClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientConfig.class);

    @Bean
    public WebClient webClient(
        @Value("${external-api.base-url}") String baseUrl,
        @Value("${external-api.timeout.connect-ms}") long connectTimeoutMs,
        @Value("${external-api.timeout.read-ms}") long readTimeoutMs
    ) {
        logger.info("Configuring WebClient with baseUrl: {}, connectTimeout: {}ms, readTimeout: {}ms",
            baseUrl, connectTimeoutMs, readTimeoutMs);

        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(readTimeoutMs))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMs);

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}

