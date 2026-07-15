package com.example.similarproducts.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record SimilarProductsResponseDto(List<ProductDetailDto> products) {

    public SimilarProductsResponseDto {
        products = products == null ? List.of() : List.copyOf(products);
    }
}