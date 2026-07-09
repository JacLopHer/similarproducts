package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/product")
@Validated
@Tag(name = "Similar Products", description = "API para consultar productos similares")
public class SimilarProductsController {

    private final GetSimilarProductsService getSimilarProductsService;

    public SimilarProductsController(GetSimilarProductsService getSimilarProductsService) {
        this.getSimilarProductsService = getSimilarProductsService;
    }

    @GetMapping("/{productId}/similar")
    @Operation(summary = "Obtener productos similares",
            description = "Retorna una lista de productos similares al producto especificado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de productos similares obtenida exitosamente",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(example = "[{\"id\":\"2\",\"name\":\"Similar Product\",\"price\":29.99,\"availability\":true}]"))),
        @ApiResponse(responseCode = "400", description = "ID de producto inválido o vacío"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<ResponseEntity<List<ProductDetailDto>>> getSimilarProducts(
            @PathVariable 
            @NotBlank(message = "El ID del producto no puede estar vacío")
            String productId) {
        return getSimilarProductsService.getSimilarProducts(productId)
            .map(response -> ResponseEntity.ok(response.products()));
    }
}

