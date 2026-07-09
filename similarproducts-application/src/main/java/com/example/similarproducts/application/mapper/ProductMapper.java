package com.example.similarproducts.application.mapper;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.dto.SimilarProductsResponseDto;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;

public class ProductMapper {

    public ProductDetailDto toDto(Product product) {
        return new ProductDetailDto(product.id(), product.name(), product.price(), product.availability());
    }

    public SimilarProductsResponseDto toResponseDto(SimilarProductsResponse response) {
        return new SimilarProductsResponseDto(response.products().stream().map(this::toDto).toList());
    }
}

