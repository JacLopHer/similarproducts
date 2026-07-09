package com.example.similarproducts.domain.model;

import java.util.List;

public record SimilarProductsResponse(List<Product> products) {

    public SimilarProductsResponse {
        products = products == null ? List.of() : List.copyOf(products);
    }
}

