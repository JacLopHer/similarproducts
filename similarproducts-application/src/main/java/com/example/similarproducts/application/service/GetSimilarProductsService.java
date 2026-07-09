package com.example.similarproducts.application.service;

import com.example.similarproducts.application.dto.SimilarProductsResponseDto;
import com.example.similarproducts.application.mapper.ProductMapper;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import com.example.similarproducts.domain.service.GetSimilarProductsUseCase;
import java.util.Objects;

public class GetSimilarProductsService {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;
    private final ProductMapper productMapper;

    public GetSimilarProductsService(GetSimilarProductsUseCase getSimilarProductsUseCase, ProductMapper productMapper) {
        this.getSimilarProductsUseCase = Objects.requireNonNull(getSimilarProductsUseCase, "getSimilarProductsUseCase is required");
        this.productMapper = Objects.requireNonNull(productMapper, "productMapper is required");
    }

    public SimilarProductsResponseDto getSimilarProducts(String productId) {
        SimilarProductsResponse response = getSimilarProductsUseCase.execute(new SimilarProductsRequest(productId));
        return productMapper.toResponseDto(response);
    }
}

