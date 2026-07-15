package com.example.similarproducts.application.service;

import com.example.similarproducts.domain.port.SimilarIdsPort;
import org.springframework.cache.annotation.Cacheable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public class SimilarIdsCacheHelper {

    private final SimilarIdsPort similarIdsPort;

    public SimilarIdsCacheHelper(SimilarIdsPort similarIdsPort) {
        this.similarIdsPort = Objects.requireNonNull(similarIdsPort, "similarIdsPort is required");
    }

    @Cacheable(value = "similar-ids", key = "#productId")
    public Mono<List<String>> getCachedSimilarIds(String productId) {
        return similarIdsPort.getSimilarIds(productId);
    }
}