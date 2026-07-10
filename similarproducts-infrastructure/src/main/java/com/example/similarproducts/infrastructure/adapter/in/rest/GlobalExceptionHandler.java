package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for consistent error handling across the API.
 * Intercepts exceptions thrown from controllers and maps them to appropriate HTTP responses.
 * Compatible with Spring WebFlux (reactive).
 *
 * <p>Handles:
 * <ul>
 *   <li>ProductNotFoundException → 404 Not Found</li>
 *   <li>InvalidProductIdException → 400 Bad Request</li>
 *   <li>ConstraintViolationException → 400 Bad Request</li>
 *   <li>Generic exceptions → 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ProductNotFoundException.
     * Returns 404 Not Found with error details.
     *
     * @param ex the ProductNotFoundException
     * @param exchange the ServerWebExchange context
     * @return ResponseEntity with Mono body containing 404 status and error details
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Mono<ErrorResponse>> handleProductNotFound(
            ProductNotFoundException ex,
            ServerWebExchange exchange) {
        String uri = exchange.getRequest().getURI().toString();
        logger.warn("ProductNotFoundException - URI: {}, Message: {}", uri, ex.getMessage());
        logger.debug("ProductNotFoundException - Full trace available in application logs");

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                uri);
    }

    /**
     * Handles InvalidProductIdException and validation constraint violations.
     * Returns 400 Bad Request with validation error details.
     *
     * @param ex the exception (either InvalidProductIdException or ConstraintViolationException)
     * @param exchange the ServerWebExchange context
     * @return ResponseEntity with Mono body containing 400 status and validation error details
     */
    @ExceptionHandler({InvalidProductIdException.class, ConstraintViolationException.class})
    public ResponseEntity<Mono<ErrorResponse>> handleBadRequest(
            Exception ex,
            ServerWebExchange exchange) {
        String errorCode = ex instanceof InvalidProductIdException
                ? "INVALID_PRODUCT_ID"
                : "VALIDATION_ERROR";
        String uri = exchange.getRequest().getURI().toString();
        logger.warn("BadRequest ({}) - URI: {}, Error: {}, Message: {}",
                errorCode, uri, ex.getClass().getSimpleName(), ex.getMessage());
        logger.debug("BadRequest - Full exception: ", ex);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                errorCode,
                ex.getMessage(),
                uri);
    }

    /**
     * Handles all unexpected exceptions.
     * Returns 500 Internal Server Error.
     *
     * @param ex the unexpected exception
     * @param exchange the ServerWebExchange context
     * @return ResponseEntity with Mono body containing 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Mono<ErrorResponse>> handleUnexpected(
            Exception ex,
            ServerWebExchange exchange) {
        String uri = exchange.getRequest().getURI().toString();
        logger.error("Unexpected error - URI: {}, Exception: {}, Message: {}",
                uri, ex.getClass().getSimpleName(), ex.getMessage());
        logger.debug("Full stack trace for debugging:", ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact support.",
                uri);
    }

    /**
     * Builds a structured error response (Reactive version).
     *
     * @param status HTTP status code
     * @param errorCode Error identifier
     * @param message Human-readable error message
     * @param uri the request URI
     * @return ResponseEntity with Mono body containing error details
     */
    private ResponseEntity<Mono<ErrorResponse>> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String uri) {
        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                message,
                uri,
                status.value());
        return ResponseEntity.status(status).body(Mono.just(errorResponse));
    }
}
