package com.example.similarproducts.infrastructure.adapter.out.client;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class SimilarIdsAdapter implements SimilarIdsPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String similarIdsEndpoint;

    public SimilarIdsAdapter(
        RestTemplate restTemplate,
        @Value("${external-api.base-url}") String baseUrl,
        @Value("${external-api.endpoints.similar-ids}") String similarIdsEndpoint
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.similarIdsEndpoint = similarIdsEndpoint;
    }

    @Override
    public List<String> getSimilarIds(String productId) {
        String url = baseUrl + similarIdsEndpoint.replace("{productId}", productId);
        try {
            String[] response = restTemplate.getForObject(url, String[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductNotFoundException("Product not found: " + productId);
        }
    }
}

