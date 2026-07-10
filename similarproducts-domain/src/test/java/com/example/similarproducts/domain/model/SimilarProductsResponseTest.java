package com.example.similarproducts.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SimilarProductsResponse Domain Model Tests")
class SimilarProductsResponseTest {

    @Test
    @DisplayName("Should create SimilarProductsResponse with valid product list")
    void shouldCreateSimilarProductsResponseWithValidProductList() {
        List<Product> products = List.of(
            new Product("1", "Product 1", new BigDecimal("10.00"), true),
            new Product("2", "Product 2", new BigDecimal("20.00"), false)
        );

        SimilarProductsResponse response = new SimilarProductsResponse(products);

        assertEquals(2, response.products().size());
        assertEquals("1", response.products().get(0).id());
        assertEquals("2", response.products().get(1).id());
    }

    @Test
    @DisplayName("Should handle null products list by converting to empty list")
    void shouldHandleNullProductsList() {
        SimilarProductsResponse response = new SimilarProductsResponse(null);

        assertNotNull(response.products());
        assertTrue(response.products().isEmpty());
        assertEquals(0, response.products().size());
    }

    @Test
    @DisplayName("Should handle empty products list")
    void shouldHandleEmptyProductsList() {
        SimilarProductsResponse response = new SimilarProductsResponse(List.of());

        assertNotNull(response.products());
        assertTrue(response.products().isEmpty());
    }

    @Test
    @DisplayName("Should make products list immutable via defensive copy")
    void shouldMakeProductsListImmutable() {
        List<Product> mutableList = new ArrayList<>();
        mutableList.add(new Product("1", "Product 1", new BigDecimal("10.00"), true));

        SimilarProductsResponse response = new SimilarProductsResponse(mutableList);

        // Modify original list
        mutableList.add(new Product("2", "Product 2", new BigDecimal("20.00"), false));

        // Response should not be affected
        assertEquals(1, response.products().size());

        // Try to modify response list directly - should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class,
                () -> response.products().add(new Product("3", "Product 3", new BigDecimal("30.00"), true))
        );
    }

    @Test
    @DisplayName("Should have correct equality based on products list")
    void shouldHaveCorrectEquality() {
        List<Product> products1 = List.of(
            new Product("1", "Product 1", new BigDecimal("10.00"), true)
        );
        List<Product> products2 = List.of(
            new Product("1", "Product 1", new BigDecimal("10.00"), true)
        );
        List<Product> products3 = List.of(
            new Product("2", "Product 2", new BigDecimal("20.00"), false)
        );

        SimilarProductsResponse response1 = new SimilarProductsResponse(products1);
        SimilarProductsResponse response2 = new SimilarProductsResponse(products2);
        SimilarProductsResponse response3 = new SimilarProductsResponse(products3);

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
    }

    @Test
    @DisplayName("Should have consistent hashCode for equal objects")
    void shouldHaveConsistentHashCode() {
        List<Product> products = List.of(
            new Product("1", "Product 1", new BigDecimal("10.00"), true)
        );

        SimilarProductsResponse response1 = new SimilarProductsResponse(products);
        SimilarProductsResponse response2 = new SimilarProductsResponse(
            List.copyOf(products)
        );

        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should create response with multiple products")
    void shouldCreateResponseWithMultipleProducts() {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            products.add(new Product(String.valueOf(i), "Product " + i,
                BigDecimal.valueOf(i * 10.0), i % 2 == 0));
        }

        SimilarProductsResponse response = new SimilarProductsResponse(products);

        assertEquals(5, response.products().size());
        assertEquals("1", response.products().get(0).id());
        assertEquals("5", response.products().get(4).id());
    }

    @Test
    @DisplayName("Should have readable toString representation")
    void shouldHaveReadableToString() {
        List<Product> products = List.of(
            new Product("1", "Product 1", new BigDecimal("10.00"), true)
        );
        SimilarProductsResponse response = new SimilarProductsResponse(products);
        String toString = response.toString();

        assertTrue(toString.contains("SimilarProductsResponse"));
    }

    @Test
    @DisplayName("Should preserve product order from input list")
    void shouldPreserveProductOrder() {
        List<Product> products = List.of(
            new Product("3", "Third", new BigDecimal("30.00"), true),
            new Product("1", "First", new BigDecimal("10.00"), false),
            new Product("2", "Second", new BigDecimal("20.00"), true)
        );

        SimilarProductsResponse response = new SimilarProductsResponse(products);

        assertEquals("3", response.products().get(0).id());
        assertEquals("1", response.products().get(1).id());
        assertEquals("2", response.products().get(2).id());
    }
}

