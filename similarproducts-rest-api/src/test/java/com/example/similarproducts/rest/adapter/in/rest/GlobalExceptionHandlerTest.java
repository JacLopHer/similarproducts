package com.example.similarproducts.rest.adapter.in.rest;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.rest.adapter.in.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

    private ServerWebExchange createMockExchange(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        var request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        var uri = mock(java.net.URI.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(uri);
        when(uri.toString()).thenReturn(path);

        return exchange;
    }

    @Test
    @DisplayName("Should map ProductNotFoundException to 404")
    void handleProductNotFound_Returns404() {
        ProductNotFoundException ex = new ProductNotFoundException("Product with id 999 not found");
        ServerWebExchange exchange = createMockExchange("/product/999/similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleProductNotFound(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(response);
        assertEquals("PRODUCT_NOT_FOUND", response.error());
        assertEquals("Product with id 999 not found", response.message());
        assertEquals(404, response.status());
        assertNotNull(response.timestamp());
        assertTrue(response.path().contains("/product/999/similar"));
    }

    @Test
    @DisplayName("Should map InvalidProductIdException to 400")
    void handleInvalidProductId_Returns400() {
        InvalidProductIdException ex = new InvalidProductIdException("Invalid product ID format");
        ServerWebExchange exchange = createMockExchange("/product/invalid/similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleBadRequest(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(response);
        assertEquals("INVALID_PRODUCT_ID", response.error());
        assertEquals("Invalid product ID format", response.message());
        assertEquals(400, response.status());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("Should map ConstraintViolationException to 400 with VALIDATION_ERROR code")
    void handleConstraintViolation_Returns400() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        when(ex.getMessage()).thenReturn("Validation failed");
        ServerWebExchange exchange = createMockExchange("/product//similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleBadRequest(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(response);
        assertEquals("VALIDATION_ERROR", response.error());
        assertEquals(400, response.status());
    }

    @Test
    @DisplayName("Should map generic Exception to 500")
    void handleUnexpectedException_Returns500() {
        RuntimeException ex = new RuntimeException("Database connection lost");
        ServerWebExchange exchange = createMockExchange("/product/1/similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleUnexpected(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(response);
        assertEquals("INTERNAL_SERVER_ERROR", response.error());
        assertTrue(response.message().contains("unexpected"));
        assertEquals(500, response.status());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("ErrorResponse should always include path and status")
    void errorResponse_IncludesPathAndStatus() {
        ProductNotFoundException ex = new ProductNotFoundException("Not found");
        ServerWebExchange exchange = createMockExchange("/product/test/similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleProductNotFound(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertNotNull(response);
        assertNotNull(response.path());
        assertTrue(response.path().contains("/product/test/similar"));
        assertEquals(404, response.status());
    }

    @Test
    @DisplayName("ErrorResponse should have non-null timestamp")
    void errorResponse_HasTimestamp() {
        InvalidProductIdException ex = new InvalidProductIdException("Invalid");
        ServerWebExchange exchange = createMockExchange("/product/test/similar");

        ResponseEntity<Mono<ErrorResponse>> responseEntity = handler.handleBadRequest(ex, exchange);
        assertNotNull(responseEntity.getBody());
        ErrorResponse response = responseEntity.getBody().block();

        assertNotNull(response);
        assertNotNull(response.timestamp());
        assertNotNull(response.message());
    }
}