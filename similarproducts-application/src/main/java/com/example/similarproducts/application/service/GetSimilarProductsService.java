package com.example.similarproducts.application.service;

import com.example.similarproducts.application.dto.SimilarProductsResponseDto;
import com.example.similarproducts.application.mapper.ProductMapper;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class GetSimilarProductsService {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;
    private final ProductMapper productMapper;

    public GetSimilarProductsService(GetSimilarProductsUseCase getSimilarProductsUseCase, ProductMapper productMapper) {
        this.getSimilarProductsUseCase = Objects.requireNonNull(getSimilarProductsUseCase, "getSimilarProductsUseCase is required");
        this.productMapper = Objects.requireNonNull(productMapper, "productMapper is required");
    }

    public Mono<SimilarProductsResponseDto> getSimilarProducts(String productId) {
        return getSimilarProductsUseCase.execute(new SimilarProductsRequest(productId))
            .map(productMapper::toResponseDto);
    }
}

