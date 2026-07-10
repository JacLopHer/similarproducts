package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

@Component
public class SimilarIdsAdapter implements SimilarIdsPort {

    private static final Logger logger = LoggerFactory.getLogger(SimilarIdsAdapter.class);

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 100;
    private static final String CACHE_KEY_PREFIX = "similar-ids:";

    private final WebClient webClient;
    private final String similarIdsEndpoint;
    private final RedisTemplate<String, List<String>> redisTemplate;
    private final long cacheTtlSeconds;

    public SimilarIdsAdapter(
        WebClient webClient,
        @Value("${external-api.base-url}${external-api.endpoints.similar-ids}") String similarIdsEndpoint,
        RedisTemplate<String, List<String>> redisTemplate,
        @Value("${cache.ttl.seconds:600}") long cacheTtlSeconds
    ) {
        this.webClient = webClient;
        this.similarIdsEndpoint = similarIdsEndpoint;
        this.redisTemplate = redisTemplate;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    @Override
    public Mono<List<String>> getSimilarIds(String productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;

        // Intenta leer del cache
        return Mono.fromCallable(() -> redisTemplate.opsForValue().get(cacheKey))
            .flatMap(cachedValue -> {
                if (cachedValue != null) {
                    logger.debug("Cache HIT: similar-ids:{}", productId);
                    return Mono.just(cachedValue);
                }
                // Cache miss - fetch from API
                return fetchFromAPI(productId)
                    .doOnNext(result -> {
                        logger.debug("Caching result for: {}", productId);
                        try {
                            redisTemplate.opsForValue().set(cacheKey, result,
                                Duration.ofSeconds(cacheTtlSeconds));
                        } catch (Exception e) {
                            logger.warn("Failed to cache result for {}: {}", productId, e.getMessage());
                        }
                    });
            })
            .switchIfEmpty(fetchFromAPI(productId)
                .doOnNext(result -> {
                    logger.debug("Caching result for: {} (from switchIfEmpty)", productId);
                    try {
                        redisTemplate.opsForValue().set(cacheKey, result,
                                Duration.ofSeconds(cacheTtlSeconds));
                    } catch (Exception e) {
                        logger.warn("Failed to cache result for {}: {}", productId, e.getMessage());
                    }
                }));
    }

    /**
     * Fetch similar IDs from external API without caching
     */
    private Mono<List<String>> fetchFromAPI(String productId) {
        logger.debug("Cache MISS: similar-ids:{}, fetching from API", productId);
        String url = similarIdsEndpoint.replace("{productId}", productId);
        logger.info("Fetching similar IDs for productId: {} from URL: {}", productId, url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .onStatus(status -> status.value() == 404,
                response -> {
                    logger.warn("Product not found (404) for productId: {}", productId);
                    return Mono.error(new ProductNotFoundException("Product not found: " + productId));
                })
            .onStatus(HttpStatusCode::is5xxServerError,
                response -> {
                    logger.error("Server error (5xx) fetching similar IDs for productId: {} - Status: {}",
                        productId, response.statusCode());
                    return Mono.error(new RuntimeException("External API server error: " + response.statusCode()));
                })
            .bodyToFlux(String.class)
            .doOnNext(id -> logger.debug("Retrieved similar ID: {} for productId: {}", id, productId))
            .doOnError(throwable -> logger.error("Error fetching similar IDs for productId {}: {}",
                productId, throwable.getClass().getSimpleName() + " - " + throwable.getMessage()))
            .retryWhen(retryPolicy())
            .onErrorMap(WebClientResponseException.NotFound.class,
                ex -> new ProductNotFoundException("Product not found: " + productId))
            .onErrorMap(this::mapNetworkException)
            .collectList();
    }

    /**
     * Política de reintentos adaptada a excepciones transitorias
     */
    private Retry retryPolicy() {
        return Retry.backoff(MAX_RETRIES, java.time.Duration.ofMillis(RETRY_DELAY_MS))
            .filter(this::isRetryableException)
            .doBeforeRetry(signal ->
                logger.warn("Retrying (attempt {}/{}) due to: {}",
                    signal.totalRetries() + 1,
                    MAX_RETRIES,
                    signal.failure().getClass().getSimpleName())
            )
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                logger.error("Max retries ({}) exhausted for similar IDs request. Last error: {}",
                    MAX_RETRIES, retrySignal.failure().getMessage());
                return retrySignal.failure();
            });
    }

    /**
     * Determina si una excepción es reintentable (transitorias)
     */
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof java.io.IOException ||
                (throwable instanceof WebClientResponseException &&
                        ((WebClientResponseException) throwable).getStatusCode().is5xxServerError());
    }

    /**
     * Mapea excepciones de red a excepciones de dominio apropiadas
     */
    private Throwable mapNetworkException(Throwable throwable) {
        if (throwable instanceof java.net.SocketTimeoutException) {
            logger.error("Socket timeout fetching similar IDs: {}", throwable.getMessage());
            return new RuntimeException("External API timeout: connection took too long", throwable);
        } else if (throwable instanceof ConnectException) {
            logger.error("Connection refused fetching similar IDs: {}", throwable.getMessage());
            return new RuntimeException("External API unavailable: connection refused", throwable);
        } else if (throwable instanceof PrematureCloseException) {
            logger.error("Connection closed prematurely fetching similar IDs: {}", throwable.getMessage());
            return new RuntimeException("External API connection closed prematurely", throwable);
        }
        return throwable;
    }
}

