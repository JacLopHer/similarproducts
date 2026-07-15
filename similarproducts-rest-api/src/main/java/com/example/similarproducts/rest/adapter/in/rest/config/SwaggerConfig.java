package com.example.similarproducts.rest.adapter.in.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** * OpenAPI/Swagger configuration for automatic REST API documentation. * Exposes documentation at: * - Swagger UI: http://localhost:5000/swagger-ui.html * - OpenAPI JSON: http://localhost:5000/v3/api-docs */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI similarProductsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Similar Products API")
                        .description("REST API to retrieve similar products using hexagonal architecture with Spring WebFlux.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Inditex Team")
                                .url("https://www.inditex.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}