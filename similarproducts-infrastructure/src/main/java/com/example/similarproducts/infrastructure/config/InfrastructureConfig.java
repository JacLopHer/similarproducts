package com.example.similarproducts.infrastructure.config;

import com.example.similarproducts.application.config.ApplicationConfig;
import com.example.similarproducts.application.mapper.ProductMapper;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import com.example.similarproducts.application.service.GetSimilarProductsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfrastructureConfig {

    @Bean
    public ApplicationConfig applicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public ProductMapper productMapper(ApplicationConfig applicationConfig) {
        return applicationConfig.productMapper();
    }

    @Bean
    public GetSimilarProductsUseCase getSimilarProductsUseCase(
        ApplicationConfig applicationConfig,
        SimilarIdsPort similarIdsPort,
        ProductDetailPort productDetailPort
    ) {
        return applicationConfig.getSimilarProductsUseCase(similarIdsPort, productDetailPort);
    }

    @Bean
    public GetSimilarProductsService getSimilarProductsService(
        ApplicationConfig applicationConfig,
        GetSimilarProductsUseCase useCase,
        ProductMapper productMapper
    ) {
        return applicationConfig.getSimilarProductsService(useCase, productMapper);
    }
}

