package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST Controller for retrieving similar products.
 * Exposes reactive endpoints to obtain similar products through WebFlux.
 */
@RestController
@RequestMapping("/product")
@Validated
@Tag(name = "Similar Products", description = "API to retrieve similar products")
public class SimilarProductsController {

    private final GetSimilarProductsService getSimilarProductsService;

    public SimilarProductsController(GetSimilarProductsService getSimilarProductsService) {
        this.getSimilarProductsService = getSimilarProductsService;
    }

    @GetMapping("/{productId}/similar")
    @Operation(summary = "Get similar products",
            description = "Returns a list of products similar to the specified product. " +
                    "Performs non-blocking (reactive) calls to external APIs with controlled concurrency.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Similar products list retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ProductDetailDto.class, type = "array"))),
        @ApiResponse(responseCode = "400", description = "Invalid or empty product ID"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<List<ProductDetailDto>>> getSimilarProducts(
            @PathVariable 
            @Parameter(description = "Unique product ID to retrieve similar products for",
                    required = true, example = "1")
            @NotBlank(message = "Product ID cannot be blank")
            String productId) {
        return getSimilarProductsService.getSimilarProducts(productId)
            .map(response -> ResponseEntity.ok(response.products()));
    }
}

