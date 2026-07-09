package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.infrastructure.mapper.AdapterMapper;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class ProductDetailAdapter implements ProductDetailPort {

    private static final Logger logger = LoggerFactory.getLogger(ProductDetailAdapter.class);

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
    public Mono<Product> getProductDetail(String productId) {
        String url = productDetailEndpoint.replace("{productId}", productId);
        logger.debug("Fetching product detail for productId: {} from URL: {}", productId, url);

        return webClient.get()
            .uri(url)
            .retrieve()
            .onStatus(status -> status.value() == 404,
                response -> Mono.error(new ProductNotFoundException("Product not found: " + productId)))
            .bodyToMono(ProductDetailResponse.class)
            .map(adapterMapper::toDomain)
            .doOnNext(product -> logger.debug("Retrieved product: {}", product))
            .doOnError(ex -> logger.error("Error fetching product detail for productId {}: {}", productId, ex.getMessage()))
            .onErrorMap(WebClientResponseException.NotFound.class, ex -> new ProductNotFoundException("Product not found: " + productId));
    }

    public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {
    }
}

