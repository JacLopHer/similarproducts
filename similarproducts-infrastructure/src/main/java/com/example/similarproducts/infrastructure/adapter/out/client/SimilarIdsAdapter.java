package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class SimilarIdsAdapter implements SimilarIdsPort {

    private static final Logger logger = LoggerFactory.getLogger(SimilarIdsAdapter.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private final WebClient webClient;
    private final String similarIdsEndpoint;

    public SimilarIdsAdapter(
        WebClient webClient,
        @Value("${external-api.base-url}${external-api.endpoints.similar-ids}") String similarIdsEndpoint
    ) {
        this.webClient = webClient;
        this.similarIdsEndpoint = similarIdsEndpoint;
    }

    @Override
    public Mono<List<String>> getSimilarIds(String productId) {
        if (productId == null || productId.isBlank()) {
            return Mono.just(List.of());
        }

        logger.debug("Fetching similar IDs for productId: {}", productId);

        return webClient.get()
                .uri(similarIdsEndpoint, productId)
                .retrieve()
                .onStatus(status -> status.value() == 404, clientResponse -> {
                    logger.warn("Product not found: {}", productId);
                    return Mono.error(new ProductNotFoundException("Product not found: " + productId));
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                    logger.error("External API server error");
                    return Mono.error(new RuntimeException("External API server error"));
                })
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .doOnNext(ids -> logger.debug("Retrieved similar IDs: {} for productId: {}", ids, productId))
                .timeout(Duration.ofSeconds(10))
                .doOnSubscribe(s -> logger.debug("Calling external API for similar IDs: {}", productId))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                        .filter(this::isRetryable)
                        .doBeforeRetry(retrySignal -> logger.debug("Retrying getSimilarIds for productId: {}, attempt: {}", productId, retrySignal.totalRetries() + 1)))
                .onErrorResume(ProductNotFoundException.class, Mono::error)
                .onErrorResume(this::mapNetworkException)
                .defaultIfEmpty(List.of());
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException.InternalServerError
                || throwable instanceof WebClientResponseException.ServiceUnavailable
                || throwable instanceof WebClientResponseException.BadGateway) {
            return true;
        }
        if (throwable instanceof IOException
                || throwable instanceof ConnectException
                || throwable.getMessage() != null && throwable.getMessage().contains("timeout")) {
            return true;
        }
        return false;
    }

    private <T> Mono<T> mapNetworkException(Throwable throwable) {
        if (throwable instanceof IOException || throwable instanceof ConnectException) {
            return Mono.error(new RuntimeException("Network error: " + throwable.getMessage(), throwable));
        }
        return Mono.error(throwable);
    }
}

