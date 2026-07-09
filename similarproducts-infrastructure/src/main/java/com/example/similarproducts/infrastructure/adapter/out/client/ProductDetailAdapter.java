package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.infrastructure.mapper.AdapterMapper;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ProductDetailAdapter implements ProductDetailPort {

    private final RestTemplate restTemplate;
    private final AdapterMapper adapterMapper;
    private final String baseUrl;
    private final String productDetailEndpoint;

    public ProductDetailAdapter(
        RestTemplate restTemplate,
        AdapterMapper adapterMapper,
        @Value("${external-api.base-url}") String baseUrl,
        @Value("${external-api.endpoints.product-detail}") String productDetailEndpoint
    ) {
        this.restTemplate = restTemplate;
        this.adapterMapper = adapterMapper;
        this.baseUrl = baseUrl;
        this.productDetailEndpoint = productDetailEndpoint;
    }

    @Override
    public Mono<Product> getProductDetail(String productId) {
        return Mono.fromCallable(() -> {
                String url = baseUrl + productDetailEndpoint.replace("{productId}", productId);
                ProductDetailResponse response = restTemplate.getForObject(url, ProductDetailResponse.class);
                if (response == null) {
                    throw new ProductNotFoundException("Product not found: " + productId);
                }
                return adapterMapper.toDomain(response);
            })
            .onErrorMap(HttpClientErrorException.NotFound.class, ex -> new ProductNotFoundException("Product not found: " + productId))
            .subscribeOn(Schedulers.boundedElastic());
    }

    public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {
    }
}

