package com.example.similarproducts.application.config;

import com.example.similarproducts.application.mapper.ProductMapper;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import com.example.similarproducts.application.service.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;

public class ApplicationConfig {

    public ProductMapper productMapper() {
        return new ProductMapper();
    }

    public GetSimilarProductsUseCase getSimilarProductsUseCase(SimilarIdsPort similarIdsPort, ProductDetailPort productDetailPort) {
        return new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);
    }

    public GetSimilarProductsService getSimilarProductsService(GetSimilarProductsUseCase useCase, ProductMapper mapper) {
        return new GetSimilarProductsService(useCase, mapper);
    }
}

