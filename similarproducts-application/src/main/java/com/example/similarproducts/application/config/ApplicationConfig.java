package com.example.similarproducts.application.config;

import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;

public class ApplicationConfig {
    public GetSimilarProductsUseCase getSimilarProductsUseCase(
            SimilarIdsPort similarIdsPort,
            ProductDetailPort productDetailPort) {
        return new GetSimilarProductsService(similarIdsPort, productDetailPort);
    }
}