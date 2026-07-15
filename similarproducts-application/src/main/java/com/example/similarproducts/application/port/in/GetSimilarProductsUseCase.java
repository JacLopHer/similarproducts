package com.example.similarproducts.application.port.in;

import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import reactor.core.publisher.Mono;

public interface GetSimilarProductsUseCase {
    Mono<SimilarProductsResponse> execute(SimilarProductsRequest request);
}