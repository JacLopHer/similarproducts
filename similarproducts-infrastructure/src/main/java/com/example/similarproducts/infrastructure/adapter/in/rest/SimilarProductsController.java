package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
@Validated
public class SimilarProductsController {

    private final GetSimilarProductsService getSimilarProductsService;

    public SimilarProductsController(GetSimilarProductsService getSimilarProductsService) {
        this.getSimilarProductsService = getSimilarProductsService;
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<List<ProductDetailDto>> getSimilarProducts(@PathVariable @NotBlank String productId) {
        return ResponseEntity.ok(getSimilarProductsService.getSimilarProducts(productId).products());
    }
}

