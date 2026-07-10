package com.example.similarproducts.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Exception Tests")
class DomainExceptionsTest {

    @Test
    @DisplayName("Should throw ProductNotFoundException with message")
    void shouldThrowProductNotFoundException() {
        String message = "Product with id 123 not found";

        ProductNotFoundException exception = new ProductNotFoundException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException and propagate correctly")
    void shouldThrowProductNotFoundExceptionCorrectly() {
        String message = "Product not found";

        assertThrows(ProductNotFoundException.class, () -> {
            throw new ProductNotFoundException(message);
        });
    }

    @Test
    @DisplayName("ProductNotFoundException should be a RuntimeException")
    void productNotFoundExceptionShouldBeRuntimeException() {
        ProductNotFoundException exception = new ProductNotFoundException("Test");

        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException with message")
    void shouldThrowInvalidProductIdException() {
        String message = "Product id format is invalid";

        InvalidProductIdException exception = new InvalidProductIdException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException and propagate correctly")
    void shouldThrowInvalidProductIdExceptionCorrectly() {
        String message = "Invalid product id format";

        assertThrows(InvalidProductIdException.class, () -> {
            throw new InvalidProductIdException(message);
        });
    }

    @Test
    @DisplayName("InvalidProductIdException should be a RuntimeException")
    void invalidProductIdExceptionShouldBeRuntimeException() {
        InvalidProductIdException exception = new InvalidProductIdException("Test");

        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void shouldHandleExceptionWithNullMessage() {
        ProductNotFoundException exception = new ProductNotFoundException(null);
        assertNull(exception.getMessage());

        InvalidProductIdException exception2 = new InvalidProductIdException(null);
        assertNull(exception2.getMessage());
    }

    @Test
    @DisplayName("Should catch ProductNotFoundException in try-catch block")
    void shouldCatchProductNotFoundExceptionInTryCatch() {
        assertDoesNotThrow(() -> {
            try {
                throw new ProductNotFoundException("Test product not found");
            } catch (ProductNotFoundException e) {
                assertEquals("Test product not found", e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Should catch InvalidProductIdException in try-catch block")
    void shouldCatchInvalidProductIdExceptionInTryCatch() {
        assertDoesNotThrow(() -> {
            try {
                throw new InvalidProductIdException("Test invalid product id");
            } catch (InvalidProductIdException e) {
                assertEquals("Test invalid product id", e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Should catch both exceptions as RuntimeException")
    void shouldCatchBothExceptionsAsRuntimeException() {
        Exception exception1 = new ProductNotFoundException("Test 1");
        Exception exception2 = new InvalidProductIdException("Test 2");

        assertInstanceOf(RuntimeException.class, exception1);
        assertInstanceOf(RuntimeException.class, exception2);
    }

    @Test
    @DisplayName("ProductNotFoundException should have different message per instance")
    void productNotFoundExceptionShouldHaveDifferentMessages() {
        ProductNotFoundException exception1 = new ProductNotFoundException("Product 1 not found");
        ProductNotFoundException exception2 = new ProductNotFoundException("Product 2 not found");

        assertNotEquals(exception1.getMessage(), exception2.getMessage());
    }

    @Test
    @DisplayName("InvalidProductIdException should have different message per instance")
    void invalidProductIdExceptionShouldHaveDifferentMessages() {
        InvalidProductIdException exception1 = new InvalidProductIdException("Invalid id: abc");
        InvalidProductIdException exception2 = new InvalidProductIdException("Invalid id: xyz");

        assertNotEquals(exception1.getMessage(), exception2.getMessage());
    }

    @Test
    @DisplayName("Exceptions should have stack trace")
    void exceptionsShouldHaveStackTrace() {
        ProductNotFoundException exception1 = new ProductNotFoundException("Test");
        InvalidProductIdException exception2 = new InvalidProductIdException("Test");

        assertNotNull(exception1.getStackTrace());
        assertNotNull(exception2.getStackTrace());
        assertTrue(exception1.getStackTrace().length > 0);
        assertTrue(exception2.getStackTrace().length > 0);
    }
}

