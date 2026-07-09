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
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class HttpClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientConfig.class);

    private static final int CONNECTION_POOL_MAX_CONNECTIONS = 50;
    private static final int CONNECTION_POOL_PENDING_ACQUIRE_MAX = 100;
    private static final long CONNECTION_POOL_ACQUIRE_TIMEOUT_MS = 5000;
    private static final long CONNECTION_MAX_IDLE_TIME_MS = 30000;

    @Bean
    public WebClient webClient(
        @Value("${external-api.base-url}") String baseUrl,
        @Value("${external-api.timeout.connect-ms}") long connectTimeoutMs,
        @Value("${external-api.timeout.read-ms}") long readTimeoutMs
    ) {
        logger.info("Configuring WebClient with baseUrl: {}, connectTimeout: {}ms, readTimeout: {}ms, " +
                "connectionPoolSize: {}, pendingAcquireMax: {}",
            baseUrl, connectTimeoutMs, readTimeoutMs, CONNECTION_POOL_MAX_CONNECTIONS, CONNECTION_POOL_PENDING_ACQUIRE_MAX);

        // Configurar ConnectionProvider con pool de conexiones y límites
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom-pool")
            .maxConnections(CONNECTION_POOL_MAX_CONNECTIONS)
            .pendingAcquireMaxCount(CONNECTION_POOL_PENDING_ACQUIRE_MAX)
            .pendingAcquireTimeout(Duration.ofMillis(CONNECTION_POOL_ACQUIRE_TIMEOUT_MS))
            .maxIdleTime(Duration.ofMillis(CONNECTION_MAX_IDLE_TIME_MS))
            .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
            .responseTimeout(Duration.ofMillis(readTimeoutMs))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMs)
            .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
            .option(io.netty.channel.ChannelOption.TCP_NODELAY, true);

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}

