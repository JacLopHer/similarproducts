package com.example.similarproducts.domain.port;

import java.util.List;

public interface SimilarIdsPort {

    List<String> getSimilarIds(String productId);
}

