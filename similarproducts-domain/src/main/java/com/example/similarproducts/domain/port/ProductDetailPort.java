package com.example.similarproducts.domain.port;

import com.example.similarproducts.domain.model.Product;
import reactor.core.publisher.Mono;

/**
 * Reactive port to obtain product details.
 */
public interface ProductDetailPort {

    Mono<Product> getProductDetail(String productId);
}

