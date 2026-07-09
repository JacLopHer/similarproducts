package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

@Component
public class SimilarIdsAdapter implements SimilarIdsPort {

    private static final Logger logger = LoggerFactory.getLogger(SimilarIdsAdapter.class);

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 100;

    private final WebClient webClient;
    private final String similarIdsEndpoint;

    public SimilarIdsAdapter(
        WebClient webClient,
        @Value("${external-api.endpoints.similar-ids}") String similarIdsEndpoint
    ) {
        this.webClient = webClient;
        this.similarIdsEndpoint = similarIdsEndpoint;
    }

    @Override
    public Flux<String> getSimilarIds(String productId) {
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
            .onErrorMap(this::mapNetworkException);
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

