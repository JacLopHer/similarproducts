package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SimilarIdsAdapter implements SimilarIdsPort {

    private static final Logger logger = LoggerFactory.getLogger(SimilarIdsAdapter.class);

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
        logger.debug("Fetching similar IDs for productId: {} from URL: {}", productId, url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .onStatus(status -> status.value() == 404,
                response -> Mono.error(new ProductNotFoundException("Product not found: " + productId)))
            .bodyToFlux(String.class)
            .doOnNext(id -> logger.debug("Retrieved similar ID: {}", id))
            .doOnError(ex -> logger.error("Error fetching similar IDs for productId {}: {}", productId, ex.getMessage()))
            .onErrorMap(WebClientResponseException.NotFound.class, ex -> new ProductNotFoundException("Product not found: " + productId));
    }
}

