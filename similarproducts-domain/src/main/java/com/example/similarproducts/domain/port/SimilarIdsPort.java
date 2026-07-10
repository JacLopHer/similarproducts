package com.example.similarproducts.domain.port;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Reactive port to recover IDs of similar products without breaking the non-blocking chain.
 * Returns a Mono with a list of IDs to facilitate caching and add resilience.
 */
public interface SimilarIdsPort {

    Mono<List<String>> getSimilarIds(String productId);
}

