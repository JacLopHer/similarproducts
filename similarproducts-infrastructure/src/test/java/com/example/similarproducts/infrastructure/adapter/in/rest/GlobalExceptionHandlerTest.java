package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Validates exception mapping to HTTP responses without needing a full Spring context.
 */
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest createMockRequest(String path) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + path);
        return request;
    }

    @Test
    @DisplayName("Should map ProductNotFoundException to 404")
    void handleProductNotFound_Returns404() {
        ProductNotFoundException ex = new ProductNotFoundException("Product with id 999 not found");
        WebRequest request = createMockRequest("/product/999/similar");

        ResponseEntity<ErrorResponse> response = handler.handleProductNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_NOT_FOUND", response.getBody().error());
        assertEquals("Product with id 999 not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
        assertNotNull(response.getBody().timestamp());
        assertTrue(response.getBody().path().contains("/product/999/similar"));
    }

    @Test
    @DisplayName("Should map InvalidProductIdException to 400")
    void handleInvalidProductId_Returns400() {
        InvalidProductIdException ex = new InvalidProductIdException("Invalid product ID format");
        WebRequest request = createMockRequest("/product/invalid/similar");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_PRODUCT_ID", response.getBody().error());
        assertEquals("Invalid product ID format", response.getBody().message());
        assertEquals(400, response.getBody().status());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should map ConstraintViolationException to 400 with VALIDATION_ERROR code")
    void handleConstraintViolation_Returns400() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        when(ex.getMessage()).thenReturn("Validation failed");
        WebRequest request = createMockRequest("/product//similar");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().error());
        assertEquals(400, response.getBody().status());
    }

    @Test
    @DisplayName("Should map generic Exception to 500")
    void handleUnexpectedException_Returns500() {
        RuntimeException ex = new RuntimeException("Database connection lost");
        WebRequest request = createMockRequest("/product/1/similar");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().error());
        assertTrue(response.getBody().message().contains("unexpected"));
        assertEquals(500, response.getBody().status());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("ErrorResponse should always include path and status")
    void errorResponse_IncludesPathAndStatus() {
        ProductNotFoundException ex = new ProductNotFoundException("Not found");
        WebRequest request = createMockRequest("/product/test/similar");

        ResponseEntity<ErrorResponse> response = handler.handleProductNotFound(ex, request);

        assertNotNull(response.getBody().path());
        assertTrue(response.getBody().path().contains("/product/test/similar"));
        assertEquals(404, response.getBody().status());
    }

    @Test
    @DisplayName("ErrorResponse should have non-null timestamp")
    void errorResponse_HasTimestamp() {
        InvalidProductIdException ex = new InvalidProductIdException("Invalid");
        WebRequest request = createMockRequest("/product/test/similar");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertNotNull(response.getBody().timestamp());
        assertNotNull(response.getBody().message());
    }
}

