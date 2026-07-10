package com.example.similarproducts.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SimilarProductsRequest Domain Model Tests")
class SimilarProductsRequestTest {

    @Test
    @DisplayName("Should create SimilarProductsRequest with valid productId")
    void shouldCreateSimilarProductsRequestWithValidProductId() {
        String productId = "123";
        SimilarProductsRequest request = new SimilarProductsRequest(productId);

        assertEquals("123", request.productId());
    }

    @Test
    @DisplayName("Should create SimilarProductsRequest with different product ids")
    void shouldCreateSimilarProductsRequestWithDifferentIds() {
        SimilarProductsRequest request1 = new SimilarProductsRequest("1");
        SimilarProductsRequest request2 = new SimilarProductsRequest("999");
        SimilarProductsRequest request3 = new SimilarProductsRequest("PROD-ABC");

        assertEquals("1", request1.productId());
        assertEquals("999", request2.productId());
        assertEquals("PROD-ABC", request3.productId());
    }

    @Test
    @DisplayName("Should be immutable record")
    void shouldBeImmutableRecord() {
        SimilarProductsRequest request1 = new SimilarProductsRequest("1");
        SimilarProductsRequest request2 = new SimilarProductsRequest("2");

        assertNotEquals(request1, request2);
        assertNotEquals(request1.productId(), request2.productId());
    }

    @Test
    @DisplayName("Should have correct equality based on productId")
    void shouldHaveCorrectEquality() {
        SimilarProductsRequest request1 = new SimilarProductsRequest("123");
        SimilarProductsRequest request2 = new SimilarProductsRequest("123");
        SimilarProductsRequest request3 = new SimilarProductsRequest("456");

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
    }

    @Test
    @DisplayName("Should have consistent hashCode for equal objects")
    void shouldHaveConsistentHashCode() {
        SimilarProductsRequest request1 = new SimilarProductsRequest("123");
        SimilarProductsRequest request2 = new SimilarProductsRequest("123");

        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should handle null productId")
    void shouldHandleNullProductId() {
        SimilarProductsRequest request = new SimilarProductsRequest(null);
        assertNull(request.productId());
    }

    @Test
    @DisplayName("Should handle empty string productId")
    void shouldHandleEmptyStringProductId() {
        SimilarProductsRequest request = new SimilarProductsRequest("");
        assertEquals("", request.productId());
    }

    @Test
    @DisplayName("Should have readable toString representation")
    void shouldHaveReadableToString() {
        SimilarProductsRequest request = new SimilarProductsRequest("123");
        String toString = request.toString();

        assertTrue(toString.contains("SimilarProductsRequest"));
        assertTrue(toString.contains("123"));
    }
}

