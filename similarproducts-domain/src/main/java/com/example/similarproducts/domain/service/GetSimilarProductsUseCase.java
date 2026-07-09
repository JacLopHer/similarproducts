package com.example.similarproducts.domain.service;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.util.List;
import java.util.Objects;

public class GetSimilarProductsUseCase {

    private final SimilarIdsPort similarIdsPort;
    private final ProductDetailPort productDetailPort;

    public GetSimilarProductsUseCase(SimilarIdsPort similarIdsPort, ProductDetailPort productDetailPort) {
        this.similarIdsPort = Objects.requireNonNull(similarIdsPort, "similarIdsPort is required");
        this.productDetailPort = Objects.requireNonNull(productDetailPort, "productDetailPort is required");
    }

    public SimilarProductsResponse execute(SimilarProductsRequest request) {
        validateRequest(request);

        List<String> similarIds = similarIdsPort.getSimilarIds(request.productId());
        if (similarIds == null || similarIds.isEmpty()) {
            return new SimilarProductsResponse(List.of());
        }

        List<Product> products = similarIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .distinct()
            .map(productDetailPort::getProductDetail)
            .toList();

        return new SimilarProductsResponse(products);
    }

    private void validateRequest(SimilarProductsRequest request) {
        if (request == null || request.productId() == null || request.productId().isBlank()) {
            throw new InvalidProductIdException("productId is required");
        }
    }
}

