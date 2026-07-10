package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.infrastructure.mapper.AdapterMapper;
import java.math.BigDecimal;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

@Component
public class ProductDetailAdapter implements ProductDetailPort {

    private static final Logger logger = LoggerFactory.getLogger(ProductDetailAdapter.class);

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 100;

    private final WebClient webClient;
    private final AdapterMapper adapterMapper;
    private final String productDetailEndpoint;

    public ProductDetailAdapter(
        WebClient webClient,
        AdapterMapper adapterMapper,
        @Value("${external-api.endpoints.product-detail}") String productDetailEndpoint
    ) {
        this.webClient = webClient;
        this.adapterMapper = adapterMapper;
        this.productDetailEndpoint = productDetailEndpoint;
    }

    @Override
    @Cacheable(value = "product-detail", key = "#productId")
    public Mono<Product> getProductDetail(String productId) {
        logger.debug("Cache MISS: product-detail:{}, fetching from API", productId);
        String url = productDetailEndpoint.replace("{productId}", productId);
        logger.info("Fetching product detail for productId: {} from URL: {}", productId, url);

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
                    logger.error("Server error (5xx) fetching product detail for productId: {} - Status: {}",
                        productId, response.statusCode());
                    return Mono.error(new RuntimeException("External API server error: " + response.statusCode()));
                })
            .bodyToMono(ProductDetailResponse.class)
            .map(adapterMapper::toDomain)
            .doOnNext(product -> logger.debug("Retrieved product: {} with id: {}", product.name(), productId))
            .doOnError(throwable -> logger.error("Error fetching product detail for productId {}: {}",
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
                logger.error("Max retries ({}) exhausted for product detail request. Last error: {}",
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
            logger.error("Socket timeout fetching product detail: {}", throwable.getMessage());
            return new RuntimeException("External API timeout: connection took too long", throwable);
        } else if (throwable instanceof ConnectException) {
            logger.error("Connection refused fetching product detail: {}", throwable.getMessage());
            return new RuntimeException("External API unavailable: connection refused", throwable);
        } else if (throwable instanceof PrematureCloseException) {
            logger.error("Connection closed prematurely fetching product detail: {}", throwable.getMessage());
            return new RuntimeException("External API connection closed prematurely", throwable);
        }
        return throwable;
    }

    public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {
    }
}

