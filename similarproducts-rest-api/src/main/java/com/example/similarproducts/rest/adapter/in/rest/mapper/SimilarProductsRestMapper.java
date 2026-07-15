<<<<<<<< HEAD:similarproducts-infrastructure/src/main/java/com/example/similarproducts/infrastructure/mapper/ProductMapper.java
package com.example.similarproducts.infrastructure.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.SimilarProductsResponseDto;
========
package com.example.similarproducts.rest.adapter.in.rest.mapper;

import com.example.similarproducts.rest.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.rest.adapter.in.rest.dto.SimilarProductsResponseDto;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import org.springframework.stereotype.Component;
>>>>>>>> develop:similarproducts-rest-api/src/main/java/com/example/similarproducts/rest/adapter/in/rest/mapper/SimilarProductsRestMapper.java


@Component
public class SimilarProductsRestMapper {

    public ProductDetailDto toDto(Product product) {
        return new ProductDetailDto(product.id(), product.name(), product.price(), product.availability());
    }

    public SimilarProductsResponseDto toResponseDto(SimilarProductsResponse response) {
        return new SimilarProductsResponseDto(response.products().stream().map(this::toDto).toList());
    }
}