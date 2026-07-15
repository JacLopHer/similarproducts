package com.example.similarproducts.rest.adapter.in.rest.mapper;

import com.example.similarproducts.rest.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.rest.adapter.in.rest.dto.SimilarProductsResponseDto;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import org.springframework.stereotype.Component;


@Component
public class SimilarProductsRestMapper {

    public ProductDetailDto toDto(Product product) {
        return new ProductDetailDto(product.id(), product.name(), product.price(), product.availability());
    }

    public SimilarProductsResponseDto toResponseDto(SimilarProductsResponse response) {
        return new SimilarProductsResponseDto(response.products().stream().map(this::toDto).toList());
    }
}