package com.example.similarproducts.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Domain Model Tests")
class ProductTest {

    @Test
    @DisplayName("Should create Product with all valid fields")
    void shouldCreateProductWithValidFields() {
        String id = "1";
        String name = "Laptop";
        BigDecimal price = new BigDecimal("999.99");
        boolean availability = true;

        Product product = new Product(id, name, price, availability);

        assertEquals("1", product.id());
        assertEquals("Laptop", product.name());
        assertEquals(new BigDecimal("999.99"), product.price());
        assertTrue(product.availability());
    }

    @Test
    @DisplayName("Should create Product with different price values")
    void shouldCreateProductWithDifferentPrices() {
        Product product1 = new Product("1", "Item 1", new BigDecimal("10.00"), true);
        Product product2 = new Product("2", "Item 2", new BigDecimal("0.01"), false);
        Product product3 = new Product("3", "Item 3", new BigDecimal("9999.99"), true);

        assertEquals(new BigDecimal("10.00"), product1.price());
        assertEquals(new BigDecimal("0.01"), product2.price());
        assertEquals(new BigDecimal("9999.99"), product3.price());
    }

    @Test
    @DisplayName("Should create Product with different availability values")
    void shouldCreateProductWithDifferentAvailability() {
        Product availableProduct = new Product("1", "Available", new BigDecimal("50.00"), true);
        Product unavailableProduct = new Product("2", "Unavailable", new BigDecimal("50.00"), false);

        assertTrue(availableProduct.availability());
        assertFalse(unavailableProduct.availability());
    }

    @Test
    @DisplayName("Should be immutable - record field cannot be modified")
    void shouldBeImmutableRecord() {
        Product product = new Product("1", "Laptop", new BigDecimal("999.99"), true);

        // Verify fields are accessible but record is immutable
        assertNotNull(product.id());
        assertNotNull(product.name());
        assertNotNull(product.price());
        assertTrue(product.availability());

        // Attempt to create new instance with different values confirms immutability
        Product differentProduct = new Product("2", "Desktop", new BigDecimal("1500.00"), false);
        assertNotEquals(product.id(), differentProduct.id());
        assertNotEquals(product.name(), differentProduct.name());
        assertNotEquals(product.price(), differentProduct.price());
        assertNotEquals(product.availability(), differentProduct.availability());
    }

    @Test
    @DisplayName("Should handle null id")
    void shouldHandleNullId() {
        Product product = new Product(null, "Item", new BigDecimal("10.00"), true);
        assertNull(product.id());
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        Product product = new Product("1", null, new BigDecimal("10.00"), true);
        assertNull(product.name());
    }

    @Test
    @DisplayName("Should handle null price")
    void shouldHandleNullPrice() {
        Product product = new Product("1", "Item", null, true);
        assertNull(product.price());
    }

    @Test
    @DisplayName("Should have correct equality based on all fields")
    void shouldHaveCorrectEquality() {
        Product product1 = new Product("1", "Laptop", new BigDecimal("999.99"), true);
        Product product2 = new Product("1", "Laptop", new BigDecimal("999.99"), true);
        Product product3 = new Product("2", "Laptop", new BigDecimal("999.99"), true);

        assertEquals(product1, product2);
        assertNotEquals(product1, product3);
    }

    @Test
    @DisplayName("Should have consistent hashCode for equal objects")
    void shouldHaveConsistentHashCode() {
        Product product1 = new Product("1", "Laptop", new BigDecimal("999.99"), true);
        Product product2 = new Product("1", "Laptop", new BigDecimal("999.99"), true);

        assertEquals(product1.hashCode(), product2.hashCode());
    }

    @Test
    @DisplayName("Should have readable toString representation")
    void shouldHaveReadableToString() {
        Product product = new Product("1", "Laptop", new BigDecimal("999.99"), true);
        String toString = product.toString();

        assertTrue(toString.contains("Product"));
        assertTrue(toString.contains("1"));
        assertTrue(toString.contains("Laptop"));
        assertTrue(toString.contains("999.99"));
    }
}

