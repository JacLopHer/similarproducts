package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for consistent error handling across the API.
 * Intercepts exceptions thrown from controllers and maps them to appropriate HTTP responses.
 *
 * <p>Handles:
 * <ul>
 *   <li>ProductNotFoundException → 404 Not Found</li>
 *   <li>InvalidProductIdException → 400 Bad Request</li>
 *   <li>ConstraintViolationException → 400 Bad Request</li>
 *   <li>Generic exceptions → 500 Internal Server Error</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ProductNotFoundException.
     * Returns 404 Not Found with error details.
     *
     * @param ex the ProductNotFoundException
     * @param request the WebRequest context
     * @return ResponseEntity with 404 status and error details
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
        ProductNotFoundException ex,
        WebRequest request) {
        logger.warn("Product not found: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            "PRODUCT_NOT_FOUND",
            ex.getMessage(),
            request);
    }

    /**
     * Handles InvalidProductIdException and validation constraint violations.
     * Returns 400 Bad Request with validation error details.
     *
     * @param ex the exception (either InvalidProductIdException or ConstraintViolationException)
     * @param request the WebRequest context
     * @return ResponseEntity with 400 status and validation error details
     */
    @ExceptionHandler({InvalidProductIdException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
        Exception ex,
        WebRequest request) {
        String errorCode = ex instanceof InvalidProductIdException 
            ? "INVALID_PRODUCT_ID" 
            : "VALIDATION_ERROR";
        logger.warn("Validation error - {}: {}", errorCode, ex.getMessage());
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            errorCode,
            ex.getMessage(),
            request);
    }

    /**
     * Handles all unexpected exceptions.
     * Returns 500 Internal Server Error.
     *
     * @param ex the unexpected exception
     * @param request the WebRequest context
     * @return ResponseEntity with 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
        Exception ex,
        WebRequest request) {
        logger.error("Unexpected error", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support.",
            request);
    }

    /**
     * Builds a structured error response.
     *
     * @param status HTTP status code
     * @param errorCode Error identifier
     * @param message Human-readable error message
     * @param request the WebRequest context
     * @return ResponseEntity with error details
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
        HttpStatus status,
        String errorCode,
        String message,
        WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            message,
            request.getDescription(false).replace("uri=", ""),
            status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}

