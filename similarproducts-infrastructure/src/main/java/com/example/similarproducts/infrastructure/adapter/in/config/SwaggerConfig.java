package com.example.similarproducts.infrastructure.adapter.in.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI similarProductsOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Similar Products API")
            .description("API para consultar productos similares")
            .version("v1"));
    }
}

