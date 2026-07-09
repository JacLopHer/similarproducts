package com.example.similarproducts.domain.port;

import reactor.core.publisher.Flux;

/**
 * Puerto reactivo para recuperar IDs de productos similares sin romper la cadena non-blocking.
 */
public interface SimilarIdsPort {

    Flux<String> getSimilarIds(String productId);
}

