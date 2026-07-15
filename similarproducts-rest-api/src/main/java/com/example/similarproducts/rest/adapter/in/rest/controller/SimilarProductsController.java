package com.example.similarproducts.rest.adapter.in.rest.controller;

import com.example.similarproducts.application.port.in.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.rest.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.rest.adapter.in.rest.dto.SimilarProductsResponseDto;
import com.example.similarproducts.rest.adapter.in.rest.mapper.SimilarProductsRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/v1/product")
@Validated
@Tag(name = "Similar Products", description = "API to retrieve similar products")
public class SimilarProductsController {

    private static final Logger logger = LoggerFactory.getLogger(SimilarProductsController.class);

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;
    private final SimilarProductsRestMapper productMapper;

    public SimilarProductsController(GetSimilarProductsUseCase getSimilarProductsUseCase, SimilarProductsRestMapper productMapper) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
        this.productMapper = productMapper;
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
    public ResponseEntity<Mono<List<ProductDetailDto>>> getSimilarProducts(
            @PathVariable
            @Parameter(description = "Unique product ID to retrieve similar products for",
                    required = true, example = "1")
            @NotBlank(message = "Product ID cannot be blank")
            @Size(max = 50, message = "Product ID must not exceed 50 characters")
            String productId) {
        logger.info("Incoming request: GET /product/{}/similar", productId);
        logger.debug("Controller - Processing request for productId: {}", productId);

        return ResponseEntity.ok(
            getSimilarProductsService.getSimilarProducts(productId)
                .map(SimilarProductsResponseDto::products)
                .doOnSuccess(products ->
                    logger.info("Request completed successfully for productId: {} - Found {} similar products",
                        productId, products.size())
                )
                .doOnError(error -> logger.error("Request failed for productId: {} - Error: {}",
                    productId, error.getClass().getSimpleName()))
                .doFinally(signalType -> logger.debug("Controller - Request processing finished for productId: {}", productId))
        );
    }
}