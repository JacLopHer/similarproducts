package com.example.similarproducts.domain.port;

import com.example.similarproducts.domain.model.Product;
import reactor.core.publisher.Mono;

/**
 * Puerto reactivo para obtener el detalle de un producto preservando una ejecución end-to-end non-blocking.
 */
public interface ProductDetailPort {

    Mono<Product> getProductDetail(String productId);
}

