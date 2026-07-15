package com.example.similarproducts.application.config;

import com.example.similarproducts.application.service.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;

public class ApplicationConfig {

    public GetSimilarProductsUseCase getSimilarProductsUseCase(SimilarIdsPort similarIdsPort, ProductDetailPort productDetailPort) {
        return new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);
    }
}