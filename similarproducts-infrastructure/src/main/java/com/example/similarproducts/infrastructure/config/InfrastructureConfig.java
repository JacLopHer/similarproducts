package com.example.similarproducts.infrastructure.config;

import com.example.similarproducts.application.config.ApplicationConfig;
import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfrastructureConfig {

    @Bean
    public ApplicationConfig applicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public GetSimilarProductsUseCase getSimilarProductsUseCase(
            ApplicationConfig applicationConfig,
            SimilarIdsPort similarIdsPort,
            ProductDetailPort productDetailPort
    ) {
        return applicationConfig.getSimilarProductsUseCase(similarIdsPort, productDetailPort);
    }
}