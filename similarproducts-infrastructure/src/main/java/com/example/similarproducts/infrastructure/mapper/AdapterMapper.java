package com.example.similarproducts.infrastructure.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.infrastructure.adapter.out.client.ProductDetailAdapter.ProductDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class AdapterMapper {

    public Product toDomain(ProductDetailResponse response) {
        return new Product(response.id(), response.name(), response.price(), response.availability());
    }
}

