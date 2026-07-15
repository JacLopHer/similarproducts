package com.example.similarproducts.application.service;

import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private static final int DEFAULT_DETAIL_CONCURRENCY = 4;

    private final ProductDetailPort productDetailPort;
    private final int detailConcurrency;
    private final SimilarIdsCacheHelper cacheHelper;  // ← Inyecta el helper

    public GetSimilarProductsService(SimilarIdsPort similarIdsPort, ProductDetailPort productDetailPort) {
        this(similarIdsPort, productDetailPort, DEFAULT_DETAIL_CONCURRENCY);
    }

    public GetSimilarProductsService(SimilarIdsPort similarIdsPort, ProductDetailPort productDetailPort, int detailConcurrency) {
        SimilarIdsPort similarIdsPort1 = Objects.requireNonNull(similarIdsPort, "similarIdsPort is required");
        this.productDetailPort = Objects.requireNonNull(productDetailPort, "productDetailPort is required");
        if (detailConcurrency <= 0) {
            throw new IllegalArgumentException("detailConcurrency must be greater than zero");
        }
        this.detailConcurrency = detailConcurrency;
        this.cacheHelper = new SimilarIdsCacheHelper(similarIdsPort1);
    }

    @Override
    public Mono<SimilarProductsResponse> execute(SimilarProductsRequest request) {
        if (request == null || request.productId() == null || request.productId().isBlank()) {
            throw new InvalidProductIdException("productId is required");
        }

        return cacheHelper.getCachedSimilarIds(request.productId())  // ← Llama al helper, no a this
                .flatMapMany(Flux::fromIterable)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .flatMapSequential(productDetailPort::getProductDetail, detailConcurrency, detailConcurrency)
                .collectList()
                .map(SimilarProductsResponse::new);
    }
}