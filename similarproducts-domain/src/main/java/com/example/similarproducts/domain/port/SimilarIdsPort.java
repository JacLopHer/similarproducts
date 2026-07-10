package com.example.similarproducts.domain.port;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Puerto reactivo para recuperar IDs de productos similares sin romper la cadena non-blocking.
 * Devuelve un Mono con una lista de IDs para facilitar el caching y agregar resiliencia.
 */
public interface SimilarIdsPort {

    Mono<List<String>> getSimilarIds(String productId);
}

